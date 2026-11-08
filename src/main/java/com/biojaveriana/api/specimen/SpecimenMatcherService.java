package com.biojaveriana.api.specimen;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationType;
import com.biojaveriana.api.specimen.config.SpecimenMatchingProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Matches plant observations to existing specimens in the database.
 *
 * <p>Matching strategy:
 * <ol>
 *   <li>Check observation type is PLANT and certainty meets threshold</li>
 *   <li>Query specimens with a scientific name that has any bidirectional substring overlap
 *       with the observed species name (fuzzy matching)</li>
 *   <li>Compute Haversine distance from observation coordinates to each candidate specimen</li>
 *   <li>Filter by max distance threshold and return the closest match</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpecimenMatcherService {
    private final SpecimenRepository specimenRepository;
    private final SpecimenMatchingProperties matchingProperties;

    /**
     * Attempt to match a plant observation to an existing specimen.
     *
     * @param observation the observation to match
     * @return the best matching specimen, or empty if no match found
     */
    public Optional<Specimen> matchPlantSpecimen(Observation observation) {
        if (!matchingProperties.isEnabled()) {
            log.debug("Specimen matching is disabled globally");
            return Optional.empty();
        }

        if (observation.getObservationType() != ObservationType.PLANT) {
            log.debug("Observation type is {} - skipping specimen matching (only PLANT observations are matched)",
                    observation.getObservationType());
            return Optional.empty();
        }

        Double similarityScore = observation.getSimilarityScore();
        Double certainty = observation.getCertainty();
        if (similarityScore == null || similarityScore < matchingProperties.getMinSimilarityScore()) {
            log.debug("Similarity score {:.4f} (certainty {:.4f}) is below threshold {:.4f} for observation {} - skipping specimen matching",
                    similarityScore != null ? similarityScore : 0.0, certainty != null ? certainty : 0.0, matchingProperties.getMinSimilarityScore(), observation.getId());
            return Optional.empty();
        }

        String observedSpecies = observation.getSpeciesCommonName();
        if (observedSpecies == null || observedSpecies.isBlank()) {
            log.debug("No species name for observation {} - skipping specimen matching", observation.getId());
            return Optional.empty();
        }

        // Stage 1: Broad DB-level search - any specimen whose scientific name contains the observed name
        List<Specimen> candidatesByName = specimenRepository.findBySpeciesScientificNameContainingIgnoreCase(observedSpecies);

        log.debug("Found {} candidate specimens for observation {} (species='{}', similarity={:.4f}, certainty={:.4f}, distanceThreshold={:.0f}m)",
                candidatesByName.size(), observation.getId(), observedSpecies, similarityScore, certainty, matchingProperties.getMaxDistanceMeters());

        if (candidatesByName.isEmpty()) {
            log.debug("No candidate specimens found for observation {} (species='{}')", observation.getId(), observedSpecies);
            return Optional.empty();
        }

        // Stage 2: Bidirectional fuzzy name check + distance computation
        List<DistanceCandidate> validCandidates = candidatesByName.stream()
                .map(specimen -> new DistanceCandidate(specimen, computeHaversineDistance(
                        observation.getLatitude(), observation.getLongitude(),
                        specimen.getLatitude(), specimen.getLongitude())))
                .filter(candidate -> passesFuzzyNameCheck(observedSpecies, candidate.specimen().getSpecies().getScientificName())
                        && candidate.distanceMeters() <= matchingProperties.getMaxDistanceMeters())
                .toList();

        log.debug("After fuzzy name + distance filtering: {} valid candidates for observation {} (species='{}')",
                validCandidates.size(), observation.getId(), observedSpecies);

        if (validCandidates.isEmpty()) {
            log.debug("No valid specimen candidates for observation {} (species='{}') after distance filtering",
                    observation.getId(), observedSpecies);
            return Optional.empty();
        }

        // Stage 3: Pick the closest match
        DistanceCandidate bestMatch = validCandidates.stream()
                .min(Comparator.comparingDouble((DistanceCandidate candidate) -> candidate.distanceMeters()))
                .orElseThrow();

        log.info("Matched observation {} to specimen {} (species='{}', distance={:.0f}m, similarity={:.4f}, certainty={:.4f})",
                observation.getId(),
                bestMatch.specimen().getId(),
                bestMatch.specimen().getSpecies().getScientificName(),
                bestMatch.distanceMeters(),
                similarityScore,
                certainty);

        return Optional.of(bestMatch.specimen());
    }

    /**
     * Check if there is a bidirectional substring overlap between the observed species name
     * and the specimen's scientific name. This handles partial/incomplete names from the model.
     */
    private boolean passesFuzzyNameCheck(String observedName, String scientificName) {
        String observedLower = observedName.toLowerCase().trim();
        String scientificLower = scientificName.toLowerCase().trim();
        return scientificLower.contains(observedLower) || observedLower.contains(scientificLower);
    }

    /**
     * Compute the Haversine distance between two geographic coordinates in meters.
     */
    private double computeHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_METERS = 6_371_000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Holds a specimen candidate along with its computed distance to the observation point.
     */
    private record DistanceCandidate(Specimen specimen, double distanceMeters) {}
}
