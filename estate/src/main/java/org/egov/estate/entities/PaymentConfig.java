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
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table
@Entity(name = "cs_ep_payment_config_v1")
public class PaymentConfig extends AuditDetails {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
			name = "UUID",
			strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "id")
	private String id;

	@Column(name = "tenant_id")
	private String tenantId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_details_id")
	private PropertyDetails propertyDetails;

	@Column(name = "no_of_months")
	private Long noOfMonths;

	@Column(name = "rate_of_interest")
	private BigDecimal rateOfInterest;

	@Column(name = "is_ground_rent")
	private Boolean isGroundRent;

	@Column(name = "ground_rent_generation_type")
	private String groundRentGenerationType;

	@Column(name = "ground_rent_bill_start_date")
	private Long groundRentBillStartDate;

	@Column(name = "ground_rent_generate_demand")
	private Long groundRentGenerateDemand;

	@Column(name = "is_intrest_applicable")
	private Boolean isIntrestApplicable;

	@Column(name = "due_date_of_payment")
	private Long dueDateOfPayment;

	@OneToMany(
            cascade =  CascadeType.ALL,
            mappedBy = "paymentConfig")
	private Set<PaymentConfigItems> paymentConfigItems = new HashSet<PaymentConfigItems>();

	@Column(name = "security_amount")
	private BigDecimal securityAmount;

}
