package com.example.backend.repository;

import com.example.backend.entity.SavedTrip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedTripRepository extends JpaRepository<SavedTrip, Long> {
}