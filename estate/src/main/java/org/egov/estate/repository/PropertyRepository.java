package org.egov.estate.repository;

import org.egov.estate.entities.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, String>{

	public Property getPropertyByFileNumber(String fileNumber);
}
