package com.biojaveriana.api.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.biojaveriana.api.observation.dto.ObservationCreateRequest;
import com.biojaveriana.api.observation.dto.ObservationJobStatusResponse;
import com.biojaveriana.api.observation.dto.ObservationListResponse;
import com.biojaveriana.api.observation.dto.ObservationResponse;
import com.biojaveriana.api.observation.dto.ObservationUploadPresignRequest;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class ObservationControllerTest {
	@Mock
	private ObservationService observationService;

	private ObservationController controller;

	@BeforeEach
	void setUp() {
		controller = new ObservationController(observationService);
	}

	@Test
	void createPresign_delegatesAndReturnsOk() {
		User user = user();
		ObservationUploadPresignRequest request = new ObservationUploadPresignRequest("image/png");
		UploadPresign presign = new UploadPresign("key", "url", "PUT", "image/png", null);
		when(observationService.createPresign(user, request)).thenReturn(presign);

		var response = controller.createPresign(user, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(presign);
	}

	@Test
	void create_delegatesAndReturnsCreated() {
		User user = user();
		ObservationCreateRequest request = ObservationCreateRequest.builder()
			.referencePictureKey("image.jpg").latitude(4.6).longitude(-74.1).waitTimeoutSeconds(0).build();
		ObservationJobStatusResponse result = ObservationJobStatusResponse.builder()
			.jobId(UUID.randomUUID()).status(JobStatus.PENDING).build();
		when(observationService.createObservation(request, user)).thenReturn(result);

		var response = controller.create(user, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isSameAs(result);
	}

	@Test
	void getJobStatusAndGetObservation_delegateWithUserAndId() {
		User user = user();
		UUID id = UUID.randomUUID();
		ObservationJobStatusResponse job = ObservationJobStatusResponse.builder().jobId(UUID.randomUUID()).build();
		ObservationResponse observation = ObservationResponse.builder().id(id).build();
		when(observationService.getJobStatus(id, user)).thenReturn(job);
		when(observationService.getObservation(id, user)).thenReturn(observation);

		assertThat(controller.getJobStatus(user, id).getBody()).isSameAs(job);
		assertThat(controller.getObservation(user, id).getBody()).isSameAs(observation);
	}

	@Test
	void listAndAdminList_forwardFiltersAndPagination() {
		User user = user();
		ObservationListResponse result = new ObservationListResponse(java.util.List.of(), null);
		when(observationService.listObservations(user, 1, 25)).thenReturn(result);
		when(observationService.listAllForAdmin(user, true, false, 1, 25)).thenReturn(result);

		assertThat(controller.list(user, 1, 25).getBody()).isSameAs(result);
		assertThat(controller.listAllForAdmin(user, true, false, 1, 25).getBody()).isSameAs(result);
		verify(observationService).listObservations(user, 1, 25);
		verify(observationService).listAllForAdmin(user, true, false, 1, 25);
	}

	@Test
	void requestRevision_requestsAndReturnsUpdatedObservation() {
		User user = user();
		UUID id = UUID.randomUUID();
		ObservationResponse observation = ObservationResponse.builder().id(id).revisionRequested(true).build();
		when(observationService.getObservation(id, user)).thenReturn(observation);

		var response = controller.requestRevision(user, id);

		verify(observationService).requestRevision(id, user);
		assertThat(response.getBody()).isSameAs(observation);
	}

	private static User user() {
		return User.builder().id(UUID.randomUUID()).username("tester").role(Role.USER).build();
	}
}
