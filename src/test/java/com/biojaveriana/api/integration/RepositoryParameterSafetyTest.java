package com.biojaveriana.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.biojaveriana.api.specimen.Species;
import com.biojaveriana.api.specimen.SpeciesRepository;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryParameterSafetyTest {

    @Autowired
    private SpeciesRepository speciesRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rnF20SqlInjectionStringsAreTreatedAsSearchData() {
        Species species = speciesRepository.save(Species.builder()
                .scientificName("Quercus humboldtii")
                .commonName("Roble")
                .family("Fagaceae")
                .build());

        String[] injectionInputs = {
            "' OR '1'='1",
            "'; DROP TABLE species; --",
            "/* comment */ Quercus",
            "Quercus%_"
        };

        for (String input : injectionInputs) {
            assertThat(speciesRepository.existsByScientificName(input)).isFalse();
        }

        assertThat(speciesRepository.findById(species.getId())).isPresent();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM species", Integer.class)).isEqualTo(1);
    }
}
