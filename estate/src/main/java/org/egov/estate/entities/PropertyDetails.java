package org.egov.estate.entities;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
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
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "cs_ep_property_details_v1")
public class PropertyDetails extends AuditDetails {

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
	@JoinColumn(name = "property_id")
	private Property property;

	/**
	 * One of PROPERTY_TYPE.LEASEHOLD or PROPERTY_TYPE.FREEHOLD defined in mdms at
	 * `data/ch/EstateServices/PropertyType.json`
	 */
	@Column(name = "property_type")
	private String propertyType;

	/**
	 * One of ALLOCATION_TYPE.AUCTION or ALLOCATION_TYPE.ALLOTMENT
	 */
	@Column(name = "type_of_allocation")
	private String typeOfAllocation;

	@Column(name = "emd_amount")
	private BigDecimal emdAmount;

	@Column(name = "emd_date")
	private Long emdDate;

	@Column(name = "mode_of_auction")
	private String modeOfAuction;

	@Column(name = "scheme_name")
	private String schemeName;

	@Column(name = "date_of_auction")
	private Long dateOfAuction;

	@Column(name = "area_sqft")
	private int areaSqft;

	@Column(name = "rate_per_sqft")
	private BigDecimal ratePerSqft;

	@Column(name = "last_noc_date")
	private Long lastNocDate;

	@Column(name = "service_category")
	private String serviceCategory;

	@Column(name = "company_name")
	private String companyName;

	@Column(name = "company_address")
	private String companyAddress;

	@Column(name = "company_registration_number")
	private String companyRegistrationNumber;

	@Column(name = "company_registration_date")
	private Long companyRegistrationDate;

	@Column(name = "property_registered_to")
	private String propertyRegisteredTo;

	@Column(name = "entity_type")
	private String entityType;

	@OneToMany(
			cascade = CascadeType.ALL,
			mappedBy = "propertyDetails"
			)
	private Set<Owner> owners = new HashSet<Owner>();

	@OneToMany(
			cascade = CascadeType.ALL,
			mappedBy = "propertyDetails"
			)
	private Set<CourtCase> courtCases = new HashSet<CourtCase>();

	@OneToOne(
            cascade =  CascadeType.ALL,
            mappedBy = "propertyDetails")
	private PaymentConfig paymentConfig;

	@OneToMany(
			cascade = CascadeType.ALL,
			mappedBy = "propertyDetails"
			)
	private Set<AuctionBidder> bidders = new HashSet<AuctionBidder>();

	@OneToOne(
            cascade =  CascadeType.ALL,
            mappedBy = "propertyDetails")	
	private Demand demand;
	
	@Column(name = "branch_type")
	private String branchType;

	@OneToOne(
            cascade =  CascadeType.ALL,
            mappedBy = "propertyDetails")
	private EstateAccount estateAccount;
}
