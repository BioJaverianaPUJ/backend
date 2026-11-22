package com.biojaveriana.api.motd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.motd.dto.FunFactCreateRequest;
import com.biojaveriana.api.motd.dto.FunFactResponse;
import com.biojaveriana.api.motd.dto.FunFactUpdateRequest;
import com.biojaveriana.api.motd.exceptions.FunFactImageLimitException;
import com.biojaveriana.api.motd.exceptions.FunFactNotFoundException;
import com.biojaveriana.api.poi.Poi;
import com.biojaveriana.api.poi.PoiRepository;
import com.biojaveriana.api.poi.PoiType;
import com.biojaveriana.api.poi.PoiZoneType;
import com.biojaveriana.api.poi.exceptions.PoiNotFoundException;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class FunFactServiceTest {

    @Mock
    private FunFactRepository funFactRepository;

    @Mock
    private FunFactImageRepository funFactImageRepository;

    @Mock
    private MotdSelectionRepository motdSelectionRepository;

    @Mock
    private PoiRepository poiRepository;

    @Mock
    private S3UrlGenerator s3UrlGenerator;

    @Mock
    private ObjectService objectService;

    private FunFactService funFactService;

    private User admin;

    @BeforeEach
    void setUp() {
        funFactService = new FunFactService(
            funFactRepository,
            funFactImageRepository,
            motdSelectionRepository,
            poiRepository,
            s3UrlGenerator,
            objectService
        );

        admin = User.builder()
            .id(UUID.randomUUID())
            .username("admin")
            .email("admin@example.com")
            .passwordHash("hash")
            .firstName("Admin")
            .lastName("User")
            .gender(Gender.OTHER)
            .role(Role.ADMIN)
            .build();
    }

    @Test
    @DisplayName("create: response must include non-null createdAt and updatedAt")
    void createShouldReturnFunFactWithTimestamps() {
        FunFactCreateRequest request = FunFactCreateRequest.builder()
            .title("La rana de los manglares")
            .description("Did you know that mangrove frogs can jump ten times their body length?")
            .build();

        when(funFactRepository.save(any(FunFact.class))).thenAnswer(inv -> {
            FunFact funFact = inv.getArgument(0);
            funFact.setId(UUID.randomUUID());
            return funFact;
        });

        FunFactResponse response = funFactService.create(admin, request);

        assertNotNull(response.createdAt(), "createdAt must not be null in the create response");
        assertNotNull(response.updatedAt(), "updatedAt must not be null in the create response");
        assertEquals(request.getTitle(), response.title());
        assertEquals(request.getDescription(), response.description());
        assertEquals(admin.getId(), response.author().id());
        verify(funFactRepository).save(any(FunFact.class));
    }

    @Test
    @DisplayName("create: image keys are persisted in the requested order")
    void createShouldPersistImagesInOrder() {
        FunFactCreateRequest request = FunFactCreateRequest.builder()
            .title("Fun fact with images")
            .description("Two pictures")
            .imageKeys(new String[] { "funfacts/admin/key1.jpg", "funfacts/admin/key2.png" })
            .build();

        when(funFactRepository.save(any(FunFact.class))).thenAnswer(inv -> {
            FunFact funFact = inv.getArgument(0);
            funFact.setId(UUID.randomUUID());
            return funFact;
        });

        funFactService.create(admin, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FunFactImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(funFactImageRepository).saveAll(captor.capture());
        List<FunFactImage> images = captor.getValue();

        assertEquals(2, images.size());
        assertEquals("funfacts/admin/key1.jpg", images.get(0).getS3Key());
        assertEquals(0, images.get(0).getOrderIndex());
        assertEquals("funfacts/admin/key2.png", images.get(1).getS3Key());
        assertEquals(1, images.get(1).getOrderIndex());
    }

    @Test
    @DisplayName("create: more than 10 images is rejected before saving")
    void createShouldRejectTooManyImages() {
        FunFactCreateRequest request = FunFactCreateRequest.builder()
            .title("Too many images")
            .description("Eleven pictures")
            .imageKeys(new String[11])
            .build();

        assertThrows(FunFactImageLimitException.class, () -> funFactService.create(admin, request));
        verify(funFactRepository, never()).save(any(FunFact.class));
    }

    @Test
    @DisplayName("create: an existing poi is embedded in the fun fact")
    void createShouldLinkExistingPoi() {
        Poi poi = buildPoi();
        when(poiRepository.findById(poi.getId())).thenReturn(Optional.of(poi));
        when(funFactRepository.save(any(FunFact.class))).thenAnswer(inv -> {
            FunFact funFact = inv.getArgument(0);
            funFact.setId(UUID.randomUUID());
            return funFact;
        });

        FunFactCreateRequest request = FunFactCreateRequest.builder()
            .title("With poi")
            .description("Linked to a poi")
            .poiId(poi.getId())
            .build();

        FunFactResponse response = funFactService.create(admin, request);

        ArgumentCaptor<FunFact> captor = ArgumentCaptor.forClass(FunFact.class);
        verify(funFactRepository).save(captor.capture());
        assertSame(poi, captor.getValue().getPoi());

        assertNotNull(response.poi());
        assertEquals(poi.getId(), response.poi().id());
    }

    @Test
    @DisplayName("create: unknown poi id is rejected before saving")
    void createShouldRejectUnknownPoi() {
        UUID unknownPoiId = UUID.randomUUID();
        when(poiRepository.findById(unknownPoiId)).thenReturn(Optional.empty());

        FunFactCreateRequest request = FunFactCreateRequest.builder()
            .title("Bad poi")
            .description("Points to a missing poi")
            .poiId(unknownPoiId)
            .build();

        assertThrows(PoiNotFoundException.class, () -> funFactService.create(admin, request));
        verify(funFactRepository, never()).save(any(FunFact.class));
    }

    @Test
    @DisplayName("update: poiId re-links the fun fact to a new poi")
    void updateShouldReLinkPoi() {
        UUID funFactId = UUID.randomUUID();
        FunFact existing = FunFact.builder()
            .id(funFactId)
            .author(admin)
            .title("Old title")
            .description("Old description")
            .hidden(false)
            .build();
        when(funFactRepository.findById(funFactId)).thenReturn(Optional.of(existing));
        when(funFactRepository.save(any(FunFact.class))).thenAnswer(inv -> inv.getArgument(0));

        Poi poi = buildPoi();
        when(poiRepository.findById(poi.getId())).thenReturn(Optional.of(poi));

        FunFactResponse response = funFactService.update(funFactId, admin,
            FunFactUpdateRequest.builder().poiId(poi.getId()).build());

        assertSame(poi, existing.getPoi());
        assertNotNull(response.poi());
        assertEquals(poi.getId(), response.poi().id());
    }

    @Test
    @DisplayName("update: unknown fun fact id throws FunFactNotFoundException")
    void updateShouldThrowWhenFunFactMissing() {
        UUID missingId = UUID.randomUUID();
        when(funFactRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(FunFactNotFoundException.class, () ->
            funFactService.update(missingId, admin, FunFactUpdateRequest.builder().title("New title").build()));
    }

    @Test
    @DisplayName("delete: removes the fun fact's selections and images before deleting it")
    void deleteShouldRemoveSelectionsAndImages() {
        UUID funFactId = UUID.randomUUID();
        FunFact existing = FunFact.builder()
            .id(funFactId)
            .author(admin)
            .title("To delete")
            .description("Description")
            .hidden(false)
            .build();
        when(funFactRepository.findById(funFactId)).thenReturn(Optional.of(existing));

        funFactService.delete(funFactId, admin);

        verify(motdSelectionRepository).deleteByFunFactId(funFactId);
        verify(funFactImageRepository).deleteByFunFactId(funFactId);
        verify(funFactRepository).delete(existing);
    }

    @Test
    @DisplayName("createPresign: generates a presigned upload under the funfacts prefix")
    void createPresignShouldUseFunFactsPrefix() {
        funFactService.createPresign(admin, "image/png");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectService).createPresign(keyCaptor.capture(), eq("image/png"));
        assertTrue(keyCaptor.getValue().startsWith("funfacts/"));
        assertTrue(keyCaptor.getValue().endsWith(".png"));
    }

    private Poi buildPoi() {
        return Poi.builder()
            .id(UUID.randomUUID())
            .creator(admin)
            .name("Cerro El Tejar")
            .geometryType(PoiType.POINT)
            .type(PoiZoneType.INTEREST_ZONE)
            .centerLatitude(4.94)
            .centerLongitude(-75.63)
            .radiusMeters(500.0)
            .build();
    }
}

