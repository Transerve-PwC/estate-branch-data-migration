package org.egov.estate.entities;



import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@Entity
@Table(name = "cs_ep_account")
public class EstateAccount{

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
			name = "UUID",
			strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "id")
	private String id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_details_id")
	private PropertyDetails propertyDetails;

	@Column(name = "remainingamount")
	@Builder.Default
	private Double remainingAmount = 0D;

	@Column(name = "remaining_since")
	@Builder.Default
	private Long remainingSince = 0L;

	//@CreatedBy
	@Column(name = "created_by")
	private String createdBy;

	//@LastModifiedBy
	@Column(name = "modified_by")
	private String lastModifiedBy;

	//@CreatedDate
	@Column(name = "created_date")
	private Long createdTime;

	//@LastModifiedDate
	@Column(name = "modified_date")
	private Long lastModifiedTime;

}

