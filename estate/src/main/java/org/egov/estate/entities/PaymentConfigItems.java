package org.egov.estate.entities;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Entity(name = "cs_ep_payment_config_items_v1")
public class PaymentConfigItems {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
			name = "UUID",
			strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "id")
    private String id;

    @JsonProperty("tenantId")
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_config_id")
    private PaymentConfig paymentConfig;

    @Column(name = "ground_rent_amount")
    private BigDecimal groundRentAmount;

    @Column(name = "ground_rent_start_month")
    private Long groundRentStartMonth;

    @Column(name = "ground_rent_end_month")
    private Long groundRentEndMonth;
}
