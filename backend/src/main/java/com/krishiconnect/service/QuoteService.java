package com.krishiconnect.service;

import java.time.Clock;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.krishiconnect.dto.BookingItemRequest;
import com.krishiconnect.dto.QuoteRequest;
import com.krishiconnect.dto.QuoteResponse;
import com.krishiconnect.entity.Booking;
import com.krishiconnect.entity.BookingItemStatus;
import com.krishiconnect.entity.BookingStatus;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.entity.Quote;
import com.krishiconnect.entity.QuoteStatus;
import com.krishiconnect.entity.Requirement;
import com.krishiconnect.entity.RequirementStatus;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.repository.EquipmentRepository;
import com.krishiconnect.repository.QuoteRepository;
import com.krishiconnect.repository.RequirementRepository;
import com.krishiconnect.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final RequirementRepository requirementRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public QuoteResponse submit(Long providerId, Long requirementId, QuoteRequest request) {
        User provider = userRepository.findById(providerId).orElseThrow(() -> new NotFoundException("User not found"));
        Requirement r = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requirement " + requirementId + " not found"));
        if (r.getStatus() != RequirementStatus.OPEN) {
            throw new ConflictException("This requirement is no longer taking quotes");
        }
        if (r.getSeeker().getId().equals(providerId)) {
            throw new BadRequestException("You can't quote on your own requirement");
        }
        Equipment equipment = null;
        if (r.getNeedType() == ResourceType.EQUIPMENT) {
            if (provider.getRole() != Role.PROVIDER) {
                throw new ForbiddenException("This farmer needs equipment; only equipment providers can quote");
            }
            if (request.equipmentId() == null) {
                throw new BadRequestException("Choose which of your machines you are offering");
            }
            equipment = equipmentRepository.findById(request.equipmentId())
                    .filter(e -> e.getOwner().getId().equals(providerId))
                    .orElseThrow(() -> new BadRequestException("Choose one of your own machines"));
            if (!equipment.isBookable()) {
                throw new BadRequestException("This machine is not live yet (paused or waiting for approval)");
            }
        } else if (provider.getRole() != Role.SPECIALIST) {
            throw new ForbiddenException("This farmer needs a skilled worker; only specialists can quote");
        }

        Quote quote = quoteRepository.findByRequirementIdAndProviderId(requirementId, providerId).orElse(null);
        if (quote != null && quote.getStatus() != QuoteStatus.WITHDRAWN) {
            throw new ConflictException("You have already sent a quote for this requirement");
        }
        if (quote == null) {
            quote = new Quote();
            quote.setRequirement(r);
            quote.setProvider(provider);
        }
        quote.setEquipment(equipment);
        quote.setAmount(request.amount());
        quote.setMessage(request.message());
        quote.setStatus(QuoteStatus.PENDING);
        quote = quoteRepository.save(quote);
        events.publishEvent(new QuoteEvent(quote.getId(), QuoteEvent.Type.RECEIVED));
        return QuoteResponse.from(quote);
    }

    @Transactional
    public QuoteResponse withdraw(Long providerId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId).orElseThrow(() -> new NotFoundException("Quote not found"));
        if (!quote.getProvider().getId().equals(providerId)) {
            throw new ForbiddenException("This is not your quote");
        }
        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new ConflictException("Only a pending quote can be withdrawn");
        }
        quote.setStatus(QuoteStatus.WITHDRAWN);
        return QuoteResponse.from(quote);
    }

    /**
     * The farmer picks one quote. The requirement row is locked first, so two quotes accepted at
     * the same moment can't both win; then the booking is created through the same locking path
     * as a normal booking (the machine might have been booked by someone else since the quote).
     * Everything is one transaction: if the booking fails, no quote is marked accepted.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuoteResponse accept(Long seekerId, Long requirementId, Long quoteId) {
        Requirement r = requirementRepository.lockById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requirement " + requirementId + " not found"));
        if (!r.getSeeker().getId().equals(seekerId)) {
            throw new ForbiddenException("This is not your requirement");
        }
        if (r.getStatus() != RequirementStatus.OPEN) {
            throw new ConflictException("You have already accepted a quote for this requirement");
        }
        Quote quote = quoteRepository.findById(quoteId)
                .filter(q -> q.getRequirement().getId().equals(requirementId))
                .orElseThrow(() -> new NotFoundException("Quote not found"));
        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new ConflictException("This quote is " + quote.getStatus().name().toLowerCase());
        }

        BookingItemRequest item = quote.getEquipment() != null
                ? new BookingItemRequest(ResourceType.EQUIPMENT, quote.getEquipment().getId(), PriceUnit.FIXED, null)
                : new BookingItemRequest(ResourceType.SPECIALIST, quote.getProvider().getId(), PriceUnit.FIXED, null);
        Booking booking = bookingService.reserve(r.getSeeker(), List.of(item), r.getStartAt(), r.getEndAt(),
                "From requirement: " + r.getTitle(), quote.getAmount());
        // the provider already agreed by quoting, so the booking is confirmed straight away
        booking.getItems().forEach(i -> {
            i.setStatus(BookingItemStatus.ACCEPTED);
            i.setRespondedAt(clock.instant());
        });
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(clock.instant());

        quote.setStatus(QuoteStatus.ACCEPTED);
        quote.setRespondedAt(clock.instant());
        quote.setBookingId(booking.getId());
        r.setStatus(RequirementStatus.ASSIGNED);
        r.setAcceptedQuoteId(quote.getId());
        events.publishEvent(new QuoteEvent(quote.getId(), QuoteEvent.Type.ACCEPTED));

        for (Quote other : quoteRepository.findByRequirement(requirementId)) {
            if (!other.getId().equals(quote.getId()) && other.getStatus() == QuoteStatus.PENDING) {
                other.setStatus(QuoteStatus.REJECTED);
                other.setRespondedAt(clock.instant());
                events.publishEvent(new QuoteEvent(other.getId(), QuoteEvent.Type.REJECTED));
            }
        }
        return QuoteResponse.from(quote);
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> mine(Long providerId) {
        return quoteRepository.findByProvider(providerId).stream().map(QuoteResponse::from).toList();
    }
}
