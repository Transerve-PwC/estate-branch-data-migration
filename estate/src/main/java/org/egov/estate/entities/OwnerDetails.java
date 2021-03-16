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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table
@Entity(name = "cs_ep_owner_details_v1")
public class OwnerDetails extends AuditDetails {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
			name = "UUID",
			strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "id")
	private String id;

	@Column(name = "tenantid")
	private String tenantId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private Owner owner;

	@Column(name = "owner_name")
	private String ownerName;

	@Column(name = "guardian_name")
	private String guardianName;

	@Column(name = "guardian_relation")
	private String guardianRelation;

	@Column(name = "mobile_number")
	private String mobileNumber;

	@Column(name = "allotment_number")
	private String allotmentNumber;

	@Column(name = "date_of_allotment")
	private Long dateOfAllotment;

	@Column(name = "possesion_date")
	private Long possesionDate;

	@Column(name = "is_previous_owner_required")
	private Boolean isPreviousOwnerRequired;

	@Column(name = "address")
	private String address;

	@Column(name = "is_director")
	private Boolean isDirector;

	@Column(name = "dob")
	private Long dob;

	@Column(name = "is_current_owner")
	private Boolean isCurrentOwner;

	@Column(name = "seller_name")
	private String sellerName;

	@Column(name = "seller_guardian_name")
	private String sellerGuardianName;

	@Column(name = "seller_relation")
	private String sellerRelation;

	@Column(name = "mode_of_transfer")
	private String modeOfTransfer;

}
