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
@Entity
@Table(name = "cs_ep_auction")
public class AuctionBidder extends AuditDetails {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
			name = "UUID",
			strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "id")
	private String id;

	@Column(name = "auction_id")
	private String auctionId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_details_id")
	private PropertyDetails propertyDetails;

	@Column(name = "bidder_name")
	private String bidderName;

	/**
	 * Initial Excel parse in Property Master / Allotment
	 */
	@Column(name = "deposited_emd_amount")
	private BigDecimal depositedEMDAmount;

	/**
	 * Initial Excel parse in Property Master / Allotment
	 */
	@Column(name = "deposit_date")
	private Long depositDate;

	/**
	 * Initial Excel parse in Property Master / Allotment
	 */
	@Column(name = "emd_validity_date")
	private Long emdValidityDate;

	@Column(name = "refund_status")
	private String refundStatus;

	@Column(name = "state")
	private String state;

	@Column(name = "action")
	private String action;


}