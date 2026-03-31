package com.claims.mvp.claim.service;

import com.claims.mvp.claim.dao.ClaimRepository;
import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.claim.dto.CreateClaimRequest;
import com.claims.mvp.claim.dto.EuContextDto;
import com.claims.mvp.claim.dto.FlightDto;
import com.claims.mvp.claim.dto.IssueDto;
import com.claims.mvp.claim.enums.IssueType;
import com.claims.mvp.eligibility.service.EligibilityService;
import com.claims.mvp.eligibility.service.EligibilityServiceImpl;
import com.claims.mvp.events.dao.EventsRepository;
import com.claims.mvp.claim.model.Claim;
import com.claims.mvp.claim.model.EuContext;
import com.claims.mvp.claim.model.Flight;
import com.claims.mvp.claim.model.Issue;
import com.claims.mvp.user.model.User;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaimServiceImplTest {

    @Test
    void createClaim_setsEligibilityAndCompensation() {
        TestFixture fx = new TestFixture();
        CreateClaimRequest request = fx.buildRequest(1800, IssueType.DELAY, 200, null, false, true, false);
        var response = fx.service.createClaim(request);

        assertThat(response.getEligible()).isTrue();
        assertThat(response.getCompensationAmount()).isEqualTo(400);
    }

    @Test
    void createClaim_delayBelowThreshold_notEligible() {
        TestFixture fx = new TestFixture();
        CreateClaimRequest request = fx.buildRequest(1800, IssueType.DELAY, 100, null, false, true, false);
        var response = fx.service.createClaim(request);

        assertThat(response.getEligible()).isFalse();
        assertThat(response.getCompensationAmount()).isEqualTo(0);
    }

    @Test
    void createClaim_cancellationNoticeWithin14Days_eligible() {
        TestFixture fx = new TestFixture();
        CreateClaimRequest request = fx.buildRequest(1800, IssueType.CANCELLATION, null, 10, false, true, true);
        var response = fx.service.createClaim(request);

        assertThat(response.getEligible()).isTrue();
        assertThat(response.getCompensationAmount()).isEqualTo(400);
    }

    @Test
    void createClaim_extraordinaryCircumstances_notEligible() {
        TestFixture fx = new TestFixture();
        CreateClaimRequest request = fx.buildRequest(1800, IssueType.DELAY, 200, null, true, true, true);
        var response = fx.service.createClaim(request);

        assertThat(response.getEligible()).isFalse();
        assertThat(response.getCompensationAmount()).isEqualTo(0);
    }

    @Test
    void createClaim_compensationTiers() {
        TestFixture fx = new TestFixture();
        var r1 = fx.service.createClaim(fx.buildRequest(1500, IssueType.DELAY, 200, null, false, true, true));
        var r2 = fx.service.createClaim(fx.buildRequest(3500, IssueType.DELAY, 200, null, false, true, true));
        var r3 = fx.service.createClaim(fx.buildRequest(4000, IssueType.DELAY, 200, null, false, true, true));

        assertThat(r1.getCompensationAmount()).isEqualTo(250);
        assertThat(r2.getCompensationAmount()).isEqualTo(400);
        assertThat(r3.getCompensationAmount()).isEqualTo(600);
    }

    private class TestFixture {
        final ClaimRepository claimRepository = mock(ClaimRepository.class);
        final UserRepository userRepository = mock(UserRepository.class);
        final ModelMapper modelMapper = mock(ModelMapper.class);
        final EligibilityService eligibilityService = new EligibilityServiceImpl();
        final EventsRepository eventsRepository = mock(EventsRepository.class);
        final ClaimServiceImpl service = new ClaimServiceImpl(
                claimRepository, userRepository, modelMapper, eligibilityService, eventsRepository);

        TestFixture() {
            User user = new User();
            user.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            when(modelMapper.map(any(FlightDto.class), eq(Flight.class))).thenAnswer(invocation -> {
                FlightDto dto = invocation.getArgument(0);
                Flight f = new Flight();
                f.setDistanceKm(dto.getDistanceKm());
                return f;
            });
            when(modelMapper.map(any(IssueDto.class), eq(Issue.class))).thenReturn(new Issue());
            when(modelMapper.map(any(EuContextDto.class), eq(EuContext.class))).thenReturn(new EuContext());
            when(modelMapper.map(any(Claim.class), eq(com.claims.mvp.claim.dto.ClaimResponse.class)))
                    .thenAnswer(invocation -> {
                        Claim c = invocation.getArgument(0);
                        com.claims.mvp.claim.dto.ClaimResponse r = new com.claims.mvp.claim.dto.ClaimResponse();
                        r.setEligible(c.getEligible());
                        r.setCompensationAmount(c.getCompensationAmount());
                        return r;
                    });
            when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        CreateClaimRequest buildRequest(int distanceKm,
                                        IssueType issueType,
                                        Integer delayMinutes,
                                        Integer cancellationNoticeDays,
                                        boolean extraordinary,
                                        boolean departureFromEu,
                                        boolean euCarrier) {
            FlightDto flightDto = new FlightDto();
            flightDto.setFlightNumber("LH123");
            flightDto.setFlightDate(LocalDate.of(2026, 3, 1));
            flightDto.setRouteFrom("FRA");
            flightDto.setRouteTo("MAD");
            flightDto.setAirline("Lufthansa");
            flightDto.setBookingRef("ABC123");
            flightDto.setDistanceKm(distanceKm);

            IssueDto issueDto = new IssueDto();
            issueDto.setType(issueType);
            issueDto.setDelayMinutes(delayMinutes);
            issueDto.setCancellationNoticeDays(cancellationNoticeDays);
            issueDto.setExtraordinaryCircumstances(extraordinary);

            EuContextDto euContextDto = new EuContextDto();
            euContextDto.setDepartureFromEu(departureFromEu);
            euContextDto.setEuCarrier(euCarrier);

            CreateClaimRequest request = new CreateClaimRequest();
            request.setUserId(1L);
            request.setFlight(flightDto);
            request.setIssue(issueDto);
            request.setEuContext(euContextDto);
            return request;
        }
    }
}
