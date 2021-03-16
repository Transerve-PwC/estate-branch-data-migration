package org.egov.estate.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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
@Table(name = "cs_ep_dueamount")
public class PropertyDueAmount {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_id")
	private Property property;
	
	@Id
	@Column(name = "file_number")
	private String fileNumber;

	//@Id
	@Column(name = "tenantid")
	private String tenantId;
	
	/*
	 * @Column(name = "owner_name") private String ownerName;
	 * 
	 * @Column(name = "mobile_number") private String mobileNumber;
	 */
	@Column(name = "balance_rent")
	private double balanceRent;
	
	@Column(name = "balance_gst")
	private double balanceGST;
	
	@Column(name = "balance_gst_penalty")
	private double balanceGSTPenalty;

	@Column(name = "balance_rent_penalty")
	private double balanceRentPenalty;

	
}