package org.egov.estate.entities;

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
@Table
@Entity(name = "cs_ep_court_case_v1")
public class CourtCase extends AuditDetails {

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_details_id")
	private PropertyDetails propertyDetails;

	@Column(name = "estate_officer_court")
	private String estateOfficerCourt;

	@Column(name = "commissioners_court")
	private String commissionersCourt;

	@Column(name = "chief_administartors_court")
	private String chiefAdministartorsCourt;

	@Column(name = "advisor_to_admin_court")
	private String advisorToAdminCourt;

	@Column(name = "honorable_district_court")
	private String honorableDistrictCourt;

	@Column(name = "honorable_high_court")
	private String honorableHighCourt;

	@Column(name = "honorable_supreme_court")
	private String honorableSupremeCourt;

}
