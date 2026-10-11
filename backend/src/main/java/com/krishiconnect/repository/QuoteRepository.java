package com.krishiconnect.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Quote;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    @Query("select q from Quote q join fetch q.provider where q.requirement.id = :requirementId order by q.amount")
    List<Quote> findByRequirement(@Param("requirementId") Long requirementId);

    Optional<Quote> findByRequirementIdAndProviderId(Long requirementId, Long providerId);

    @Query("select q from Quote q join fetch q.requirement where q.provider.id = :providerId order by q.createdAt desc")
    List<Quote> findByProvider(@Param("providerId") Long providerId);

    /** [requirementId, number of live quotes] for the board cards. */
    @Query("""
            select q.requirement.id, count(q) from Quote q
            where q.requirement.id in :ids and q.status in (com.krishiconnect.entity.QuoteStatus.PENDING,
                                                             com.krishiconnect.entity.QuoteStatus.ACCEPTED)
            group by q.requirement.id
            """)
    List<Object[]> countByRequirementIds(@Param("ids") Collection<Long> ids);
}
