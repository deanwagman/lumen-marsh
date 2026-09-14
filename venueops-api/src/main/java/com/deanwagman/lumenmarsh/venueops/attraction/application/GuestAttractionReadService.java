package com.deanwagman.lumenmarsh.venueops.attraction.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionExperienceProfile;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class GuestAttractionReadService {

    private final AttractionRepository attractionRepository;
    private final AttractionExperienceRepository experienceRepository;

    public GuestAttractionReadService(
            AttractionRepository attractionRepository,
            AttractionExperienceRepository experienceRepository
    ) {
        this.attractionRepository = Objects.requireNonNull(attractionRepository);
        this.experienceRepository = Objects.requireNonNull(experienceRepository);
    }

    public List<AttractionDetail> listForGuest() {
        return attractionRepository.findAll().stream()
                .map(this::composeDetail)
                .sorted(Comparator.comparing(detail -> detail.operational().name()))
                .toList();
    }

    public AttractionDetail getDetail(AttractionId id) {
        Attraction operational = attractionRepository.findById(id)
                .orElseThrow(() -> new AttractionNotFoundException(id));
        return composeDetail(operational);
    }

    private AttractionDetail composeDetail(Attraction operational) {
        AttractionExperienceProfile experience = experienceRepository.findByAttractionId(operational.id())
                .orElseThrow(() -> new AttractionNotFoundException(operational.id()));
        return new AttractionDetail(operational, experience);
    }

    public record AttractionDetail(Attraction operational, AttractionExperienceProfile experience) {
        public AttractionDetail {
            Objects.requireNonNull(operational, "operational is required");
            Objects.requireNonNull(experience, "experience is required");
        }
    }
}
