package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.UserApartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserApartmentRepository extends JpaRepository<UserApartment, Integer> {
}
