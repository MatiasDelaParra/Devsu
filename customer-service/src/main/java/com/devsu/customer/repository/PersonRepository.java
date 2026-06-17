package com.devsu.customer.repository;

import com.devsu.customer.domain.Person;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByIdentification(String identification);

    boolean existsByIdentification(String identification);
}
