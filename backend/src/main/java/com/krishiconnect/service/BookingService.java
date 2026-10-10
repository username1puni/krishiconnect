package com.krishiconnect.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.krishiconnect.dto.BookingItemRequest;
import com.krishiconnect.dto.BookingResponse;
import com.krishiconnect.dto.BusyRange;
import com.krishiconnect.dto.CreateBookingRequest;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.entity.Booking;
import com.krishiconnect.entity.BookingItem;
import com.krishiconnect.entity.BookingItemStatus;
import com.krishiconnect.entity.BookingKind;
import com.krishiconnect.entity.BookingStatus;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.exception.ResourceUnavailableException;
import com.krishiconnect.repository.BookingItemRepository;
import com.krishiconnect.repository.BookingRepository;
import com.krishiconnect.repository.EquipmentRepository;
import com.krishiconnect.repository.SpecialistProfileRepository;
import com.krishiconnect.repository.UserRepository;
import com.krishiconnect.security.AuthUser;

import lombok.extern.slf4j.Slf4j;

/**
 * All booking state changes go through here.
 *
 * Creating a booking: lock the equipment rows (id order), then the specialist profile rows
 * (id order), check for overlapping live bookings, then insert the booking, its items and the
 * hourly availability slots. Anyone else trying to book the same tractor waits on the row lock
 * and then sees the new booking. The unique key on availability_slots backs this up.
 *
 * Changing a booking (accept, reject, cancel, complete, expire): lock the bookings row first.
 *
 * Booking creation runs in READ COMMITTED. Under MySQL's default REPEATABLE READ, the overlap
 * check could read from a snapshot taken before we got the lock and miss a booking that was
 * committed while we were waiting.
 */
@Slf4j
@Service
public class BookingService {

    /** Item states that block the calendar. */
    public static final Set<BookingItemStatus> BLOCKING = EnumSet.of(BookingItemStatus.PENDING,
            BookingItemStatus.ACCEPTED, BookingItemStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final BookingItemRepository itemRepository;
    private final EquipmentRepository equipmentRepository;
    private final SpecialistProfileRepository specialistRepository;
    private final UserRepository userRepository;
    private final PricingCalculator pricing;
    private final SlotWriter slotWriter;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration pendingTimeout;
    private final int maxDays;

    public BookingService(BookingRepository bookingRepository, BookingItemRepository itemRepository,
                          EquipmentRepository equipmentRepository, SpecialistProfileRepository specialistRepository,
                          UserRepository userRepository, PricingCalculator pricing, SlotWriter slotWriter,
                          ApplicationEventPublisher events, Clock clock,
                          @Value("${app.booking.pending-minutes}") long pendingMinutes,
                          @Value("${app.booking.max-days}") int maxDays) {
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.equipmentRepository = equipmentRepository;
        this.specialistRepository = specialistRepository;
        this.userRepository = userRepository;
        this.pricing = pricing;
        this.slotWriter = slotWriter;
        this.events = events;
        this.clock = clock;
        this.pendingTimeout = Duration.ofMinutes(pendingMinutes);
        this.maxDays = maxDays;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse create(Long seekerId, CreateBookingRequest request) {
        User seeker = userRepository.findById(seekerId).orElseThrow(() -> new NotFoundException("User not found"));
        Booking booking = reserve(seeker, request.items(), request.startAt(), request.endAt(), request.note(), null);

        Instant now = clock.instant();
        Instant startsAt = request.startAt().atZone(clock.getZone()).toInstant();
        // never keep a request pending past the time the work should begin
        booking.setExpiresAt(now.plus(pendingTimeout).isBefore(startsAt) ? now.plus(pendingTimeout) : startsAt);
        events.publishEvent(new BookingEvent(booking.getId(), BookingEvent.Type.REQUESTED, null));
        return BookingResponse.from(booking);
    }

    /**
     * Locks the resources, checks the calendar and saves a PENDING booking with its slots.
     * When agreedAmount is given (an accepted quotation) the single item is priced at that amount.
     * Must run inside a READ COMMITTED transaction.
     */
    public Booking reserve(User seeker, List<BookingItemRequest> requested, LocalDateTime start, LocalDateTime end,
                           String note, BigDecimal agreedAmount) {
        validateRange(start, end);
        Set<String> seen = new HashSet<>();
        for (BookingItemRequest r : requested) {
            if (!seen.add(r.type() + ":" + r.resourceId())) {
                throw new BadRequestException("The same item was added twice");
            }
        }
        List<Long> equipmentIds = requested.stream().filter(r -> r.type() == ResourceType.EQUIPMENT)
                .map(BookingItemRequest::resourceId).sorted().toList();
        List<Long> specialistIds = requested.stream().filter(r -> r.type() == ResourceType.SPECIALIST)
                .map(BookingItemRequest::resourceId).sorted().toList();

        // 1. row locks, always equipment before specialists and lowest id first
        Map<Long, Equipment> equipment = equipmentIds.isEmpty() ? Map.of()
                : equipmentRepository.lockAllByIds(equipmentIds).stream()
                        .collect(Collectors.toMap(Equipment::getId, Function.identity()));
        Map<Long, SpecialistProfile> specialists = specialistIds.isEmpty() ? Map.of()
                : specialistRepository.lockAllByIds(specialistIds).stream()
                        .collect(Collectors.toMap(SpecialistProfile::getUserId, Function.identity()));

        // 2. build the items and check each one is free for the whole range
        Booking booking = new Booking();
        booking.setSeeker(seeker);
        booking.setKind(requested.size() > 1 ? BookingKind.BUNDLE : BookingKind.SINGLE);
        booking.setStartAt(start);
        booking.setEndAt(end);
        booking.setNote(note);
        List<String> taken = new ArrayList<>();
        for (BookingItemRequest r : requested) {
            BookingItem item = r.type() == ResourceType.EQUIPMENT
                    ? equipmentItem(seeker, equipment.get(r.resourceId()), r, start, end, agreedAmount)
                    : specialistItem(seeker, specialists.get(r.resourceId()), r, start, end, agreedAmount);
            boolean busy = r.type() == ResourceType.EQUIPMENT
                    ? itemRepository.existsEquipmentOverlap(r.resourceId(), start, end, BLOCKING)
                    : itemRepository.existsSpecialistOverlap(r.resourceId(), start, end, BLOCKING);
            if (busy) {
                taken.add(item.resourceName());
            }
            booking.addItem(item);
        }
        if (!taken.isEmpty()) {
            // nothing has been written yet, so a bundle with one busy part books nothing
            throw new ResourceUnavailableException(taken);
        }

        // 3. save; the slot insert fails on the unique key if anything slipped past the lock
        bookingRepository.saveAndFlush(booking);
        for (BookingItem item : booking.getItems()) {
            slotWriter.reserve(item);
        }
        log.info("Booking {} created by user {} for {} item(s)", booking.getId(), seeker.getId(), booking.getItems().size());
        return booking;
    }

    private BookingItem equipmentItem(User seeker, Equipment e, BookingItemRequest r, LocalDateTime start,
                                      LocalDateTime end, BigDecimal agreedAmount) {
        if (e == null || !e.isBookable()) {
            throw new NotFoundException("Equipment " + r.resourceId() + " is not available for booking");
        }
        Long ownerId = e.getOwner().getId();
        if (ownerId.equals(seeker.getId())) {
            throw new BadRequestException("You can't book your own equipment");
        }
        BookingItem item = new BookingItem();
        item.setResourceType(ResourceType.EQUIPMENT);
        item.setEquipment(e);
        item.setProvider(userRepository.getReferenceById(ownerId));
        price(item, r, e.priceFor(r.priceUnit()), start, end, agreedAmount);
        return item;
    }

    private BookingItem specialistItem(User seeker, SpecialistProfile p, BookingItemRequest r, LocalDateTime start,
                                       LocalDateTime end, BigDecimal agreedAmount) {
        if (p == null || !p.isAvailable()) {
            throw new NotFoundException("This specialist is not taking bookings right now");
        }
        User person = userRepository.findById(p.getUserId()).orElseThrow();
        if (!person.isVerified() || !person.isEnabled()) {
            throw new NotFoundException("This specialist is not taking bookings right now");
        }
        if (person.getId().equals(seeker.getId())) {
            throw new BadRequestException("You can't book yourself");
        }
        if (r.priceUnit() == PriceUnit.ACRE) {
            throw new BadRequestException("Specialists are booked per hour or per day");
        }
        BigDecimal rate = r.priceUnit() == PriceUnit.HOUR ? p.getRatePerHour() : p.getRatePerDay();
        BookingItem item = new BookingItem();
        item.setResourceType(ResourceType.SPECIALIST);
        item.setSpecialist(person);
        item.setProvider(person);
        price(item, r, rate, start, end, agreedAmount);
        return item;
    }

    private void price(BookingItem item, BookingItemRequest r, BigDecimal rate, LocalDateTime start,
                       LocalDateTime end, BigDecimal agreedAmount) {
        PricingCalculator.Price price = agreedAmount != null
                ? pricing.price(PriceUnit.FIXED, agreedAmount, start, end, null)
                : pricing.price(r.priceUnit(), rate, start, end, r.acres());
        item.setPriceUnit(agreedAmount != null ? PriceUnit.FIXED : r.priceUnit());
        item.setStartAt(start);
        item.setEndAt(end);
        item.setQuantity(price.quantity());
        item.setUnitPrice(price.unitPrice());
        item.setAmount(price.amount());
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BadRequestException("Choose a start and end time");
        }
        if (start.getMinute() != 0 || start.getSecond() != 0 || end.getMinute() != 0 || end.getSecond() != 0) {
            throw new BadRequestException("Bookings start and end on the hour");
        }
        if (!end.isAfter(start)) {
            throw new BadRequestException("End time must be after the start time");
        }
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Start time must be in the future");
        }
        if (Duration.between(start, end).toDays() >= maxDays) {
            throw new BadRequestException("A booking can be at most " + maxDays + " days long");
        }
    }

    @Transactional
    public BookingResponse acceptItem(Long providerId, Long itemId) {
        BookingItem item = providerItem(providerId, itemId);
        Booking booking = lockOpenBooking(item.getBooking().getId(), BookingStatus.PENDING);
        if (booking.getExpiresAt() != null && !booking.getExpiresAt().isAfter(clock.instant())) {
            throw new ConflictException("This request has already expired");
        }
        if (item.getStatus() != BookingItemStatus.PENDING) {
            throw new ConflictException("You have already answered this request");
        }
        item.setStatus(BookingItemStatus.ACCEPTED);
        item.setRespondedAt(clock.instant());

        boolean allAccepted = booking.getItems().stream().allMatch(i -> i.getStatus() == BookingItemStatus.ACCEPTED);
        if (allAccepted) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(clock.instant());
            booking.setExpiresAt(null);
            events.publishEvent(new BookingEvent(booking.getId(), BookingEvent.Type.CONFIRMED, itemId));
        } else {
            events.publishEvent(new BookingEvent(booking.getId(), BookingEvent.Type.ITEM_ACCEPTED, itemId));
        }
        return BookingResponse.from(booking);
    }

    /** A bundle is all-or-nothing: one provider saying no releases every item. */
    @Transactional
    public BookingResponse rejectItem(Long providerId, Long itemId) {
        BookingItem item = providerItem(providerId, itemId);
        Booking booking = lockOpenBooking(item.getBooking().getId(), BookingStatus.PENDING);
        close(booking, BookingStatus.REJECTED, BookingItemStatus.REJECTED);
        events.publishEvent(new BookingEvent(booking.getId(), BookingEvent.Type.REJECTED, itemId));
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse cancel(Long seekerId, Long bookingId) {
        Booking booking = bookingRepository.lockById(bookingId).orElseThrow(() -> notFound(bookingId));
        if (!booking.getSeeker().getId().equals(seekerId)) {
            throw new ForbiddenException("This is not your booking");
        }
        if (!booking.isOpen()) {
            throw new ConflictException("This booking is already " + booking.getStatus().name().toLowerCase());
        }
        if (!booking.getStartAt().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("The booking has already started and can't be cancelled");
        }
        close(booking, BookingStatus.CANCELLED, BookingItemStatus.CANCELLED);
        events.publishEvent(new BookingEvent(booking.getId(), BookingEvent.Type.CANCELLED, null));
        return BookingResponse.from(booking);
    }

    /** Provider marks the work done once it has started. Reviews open after that. */
    @Transactional
    public BookingResponse completeItem(Long providerId, Long itemId) {
        BookingItem item = providerItem(providerId, itemId);
        Booking booking = lockOpenBooking(item.getBooking().getId(), BookingStatus.CONFIRMED);
        if (LocalDateTime.now(clock).isBefore(item.getStartAt())) {
            throw new ConflictException("You can mark it done once the work has started");
        }
        if (item.getStatus() != BookingItemStatus.ACCEPTED) {
            throw new ConflictException("This item is already " + item.getStatus().name().toLowerCase());
        }
        item.setStatus(BookingItemStatus.COMPLETED);
        if (booking.getItems().stream().allMatch(i -> i.getStatus() == BookingItemStatus.COMPLETED)) {
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setClosedAt(clock.instant());
            events.publishEvent(new BookingEvent(booking.getId(), BookingEvent.Type.COMPLETED, itemId));
        }
        return BookingResponse.from(booking);
    }

    /**
     * Called by the expiry job, one booking per transaction. Re-checks the state under the lock,
     * because the provider may have accepted it a moment ago.
     */
    @Transactional
    public boolean expire(Long bookingId) {
        Booking booking = bookingRepository.lockById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING || booking.getExpiresAt() == null
                || booking.getExpiresAt().isAfter(clock.instant())) {
            return false;
        }
        close(booking, BookingStatus.EXPIRED, BookingItemStatus.EXPIRED);
        events.publishEvent(new BookingEvent(booking.getId(), BookingEvent.Type.EXPIRED, null));
        return true;
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredIds(int limit) {
        return bookingRepository.findIdsToExpire(BookingStatus.PENDING, clock.instant(), PageRequest.of(0, limit));
    }

    private void close(Booking booking, BookingStatus status, BookingItemStatus itemStatus) {
        booking.setStatus(status);
        booking.setClosedAt(clock.instant());
        booking.setExpiresAt(null);
        for (BookingItem i : booking.getItems()) {
            if (i.getStatus() == BookingItemStatus.PENDING || i.getStatus() == BookingItemStatus.ACCEPTED) {
                i.setStatus(itemStatus);
            }
        }
        slotWriter.release(booking.getItems().stream().map(BookingItem::getId).toList());
    }

    private BookingItem providerItem(Long providerId, Long itemId) {
        BookingItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Booking item " + itemId + " not found"));
        if (!item.getProvider().getId().equals(providerId)) {
            throw new ForbiddenException("This request was sent to someone else");
        }
        return item;
    }

    private Booking lockOpenBooking(Long bookingId, BookingStatus expected) {
        Booking booking = bookingRepository.lockById(bookingId).orElseThrow(() -> notFound(bookingId));
        if (booking.getStatus() != expected) {
            throw new ConflictException("This booking is already " + booking.getStatus().name().toLowerCase());
        }
        return booking;
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long bookingId, AuthUser viewer) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> notFound(bookingId));
        boolean allowed = viewer.isAdmin() || booking.getSeeker().getId().equals(viewer.getId())
                || booking.getItems().stream().anyMatch(i -> i.getProvider().getId().equals(viewer.getId()));
        if (!allowed) {
            throw new ForbiddenException("This is not your booking");
        }
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> mine(Long seekerId, BookingStatus status, int page, int size) {
        var result = bookingRepository.findBySeeker(seekerId, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, BookingResponse::from);
    }

    /** Requests sent to me as an equipment owner or specialist, with the booking around them. */
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> incoming(Long providerId, BookingItemStatus status, int page, int size) {
        var items = itemRepository.findForProvider(providerId, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(items, i -> BookingResponse.from(i.getBooking()));
    }

    @Transactional(readOnly = true)
    public List<BusyRange> busy(ResourceType type, Long resourceId, LocalDate from, int days) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = start.plusDays(Math.min(Math.max(days, 1), 62));
        List<BookingItem> items = type == ResourceType.EQUIPMENT
                ? itemRepository.findEquipmentBusy(resourceId, start, end, BLOCKING)
                : itemRepository.findSpecialistBusy(resourceId, start, end, BLOCKING);
        return items.stream()
                .sorted(Comparator.comparing(BookingItem::getStartAt))
                .map(i -> new BusyRange(i.getStartAt(), i.getEndAt(), i.getStatus()))
                .toList();
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Booking " + id + " not found");
    }
}
