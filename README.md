
ServiceManager serviceManager = new ServiceManager();
BOFactory boFactory = (BOFactory)serviceManager.locateService("com/ibm/websphere/bo/BOFactory");
ArrayList transList = new ArrayList();
int notificationId = OCAIConstants.ZERO;
String targetSystemCode = OCAIConstants.EMPTY_STRING;
int partyContactDetails = OCAIConstants.ZERO; // holds the count of the party contacts
int partyInvolvedInClaim = OCAIConstants.ZERO; // holds the count of the parties involved in the claim
int thirdPartyVehicles = OCAIConstants.ZERO; // holds the number of third party vehicles involved in the claim
int passenger = OCAIConstants.ZERO; // holds the current passenger array index
int witness = OCAIConstants.ZERO; // holds the current witness array index
int noOfInsured = OCAIConstants.ZERO;
ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
StringBuilder lossLocation = new StringBuilder();
StringBuilder addressDetails = new StringBuilder();
int partyInvolvedInPolicy = OCAIConstants.ZERO; // holds the count of the parties involved in the policy
String contactName = OCAIConstants.EMPTY_STRING; // holds the value for Legis Contact Name
String InsuredName = OCAIConstants.EMPTY_STRING;	// holds the value for Legis Insured Name
String BrokerName = OCAIConstants.EMPTY_STRING;	// holds the value for Legis Broker Name
int insuredAddressFlag = OCAIConstants.ZERO; // flag to check whether Insured Address is available or not
int noOfAddresses = OCAIConstants.ZERO; // holds the no of addresses in Policy Snapshot response

com.chartis.xtrans.TransDAO transDAO = new com.chartis.xtrans.TransDAO(); //new TransDAO object
String servicesOutput = OCAIConstants.EMPTY_STRING; //holds the RDF response
String services = OCAIConstants.EMPTY_STRING; // holds the OCFE services value
String servicesOutputValue = OCAIConstants.EMPTY_STRING; // holds the Legis services value

Logger logger = new GCILogger().getGCILogger("CHARTIS_OCAI ClaimSubrogationUKBusinessProcess", OCAIConstants.CLAIM_SUBROGATION_UK_LOG);
try {
	logger.log(Level.INFO, "Preparing Subrogation Request");
	BOXMLSerializer serializer = (BOXMLSerializer) ServiceManager.INSTANCE.locateService("com/ibm/websphere/bo/BOXMLSerializer");
	//allocateSubrogationActivityResponse = boFactory.create("http://www.ibm.com/ima/IMAXML/service/ClaimSubrogationService", "AllocateSubrogationActivityResponse");
	legisOutboundRequest = boFactory.create("http://www.ibm.com/xmlns/prod/websphere/j2ca/jdbc/kbondbatoutbound_stagingbg", "KbondbaToutbound_StagingBG");
	DataObject legisOutbound = boFactory.create("http://www.ibm.com/xmlns/prod/websphere/j2ca/jdbc/kbondbatoutbound_staging", "KbondbaToutbound_Staging");
	DataObject recordClaimDetailsRequestBO = boFactory.create("http://gci.chartis.com/legis/ClaimDetails", "RecordClaimDetailsRequest");
	bpelError = OCAIConstants.EMPTY_STRING;
	errorInfo = boFactory.create("http://www.ibm.com/ima/IMAXML", "ErrorInfo");
	
	XTransform xTransForm = new XTransform();
	
	if(null != allocateSubrogationActivityRequest.getDataObject("applicationContext")) {
		targetSystemCode = allocateSubrogationActivityRequest.getString("applicationContext/requestType");
	}
	if(null != allocateSubrogationActivityRequest.getDataObject("lossEvent")) {
		/*Accident Details*/
		transList.add(new XTransMap(new XElement("lossEvent/startDate"),
					new XElement("/AccidentDetails/LossDate"),false));
		transList.add(new XTransMap(new XElement("lossEvent/eventTime"),
					new XElement("/AccidentDetails/LossTime"),false));
		//For Loss Location
        transList.add(new XTransMap(new XElement("lossEvent/roadType", "ROAD_TYPE"),
                    new XElement("/AccidentDetails/LossLocation"), OCAIConstants.APPEND_WITH_CODE_TO_DESCRIPTION));
        /*transList.add(new XTransMap(new XElement("lossEvent/street"),
                    new XElement("/AccidentDetails/LossLocation"), OCAIConstants.APPEND_WITHOUT_ALL));
        transList.add(new XTransMap(new XElement("lossEvent/city"),
                    new XElement("/AccidentDetails/LossLocation"), OCAIConstants.APPEND_WITHOUT_ALL));
        transList.add(new XTransMap(new XElement("lossEvent/postalCode"),
                    new XElement("/AccidentDetails/LossLocation"), OCAIConstants.APPEND_WITHOUT_ALL));*/
		
		transList.add(new XTransMap(new XElement("lossEvent/description"),
					new XElement("/AccidentDetails/ExplanatoryNotes"),false));
		transList.add(new XTransMap(new XElement("lossEvent/condition/description", "ROAD_CONDITION"),
					new XElement("/AccidentDetails/RoadConditions"),OCAIConstants.REQUIRES_CODE_TO_DESCRIPTION));
		transList.add(new XTransMap(new XElement("lossEvent/visibility", "WEATHER"),
					new XElement("/AccidentDetails/WeatherConditions"),OCAIConstants.REQUIRES_CODE_TO_DESCRIPTION));
					
		/*CR121: Start Changes - Adding Loss Country  */
		if(null != allocateSubrogationActivityRequest.getString("lossEvent/lossCountry")){
			transList.add(new XTransMap(new XElement("lossEvent/lossCountry"),
				new XElement("/AccidentDetails/LossCountry"),false));
		}
	}
	
	if(null != allocateSubrogationActivityRequest.getDataObject("subrogatedClaim")) {
		if(null != allocateSubrogationActivityRequest.getDataObject("subrogatedClaim/compositeClaimFolder")) {
			notificationId = allocateSubrogationActivityRequest.getInt("subrogatedClaim/compositeClaimFolder/notificationId");
		}
		transList.add(new XTransMap(new XElement("subrogatedClaim/underlyingAgreements[1]/externalReference"), 
					new XElement("/ClaimDetails/PolicyNumber"),false));
		transList.add(new XTransMap(new XElement("subrogatedClaim/externalReference"), 
					new XElement("/ClaimDetails/ClaimNo"),false));
		
		/*CR121: Start Changes - Adding Liability Decision & AdjusterId  */
		if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/accidentFault")){
		transList.add(new XTransMap(new XElement("subrogatedClaim/accidentFault"), 
					new XElement("/ClaimDetails/LiabilityDecision"),false));
					}
		if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/adjusterId")){
		transList.add(new XTransMap(new XElement("subrogatedClaim/adjusterId"), 
					new XElement("/PolicyDetails/Adjuster"),false));
					}
					
		/*Parties involved in Claim*/
		partyInvolvedInClaim = allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim")!=null? allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim").size():OCAIConstants.ZERO;
		for(int partyNo=1;partyNo<=partyInvolvedInClaim;partyNo++) {
			if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName")
				&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName").equalsIgnoreCase("INSURED")) {
				
				noOfInsured++;
				logger.log(Level.INFO, "Insured Vehicle " + noOfInsured);
				/* Insured General Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/prefixTitles"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Title"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/firstName"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/FirstName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/lastName"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/LastName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/gender", "GENDER"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Gender", "GENDER"),true));
	
				/* Insured Address Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[1]"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Address/AddressLine1"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[2]"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Address/AddressLine2"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/city"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Address/City"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/county"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Address/County"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/country"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Address/Country"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/postalCode"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/Address/PostCode"),false));
	
				/* Insured Contact Details */
				partyContactDetails = allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber") != null? 
												allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber").size():OCAIConstants.ZERO;
				for(int contact=1;contact<=partyContactDetails;contact++) {
					if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("BUSINESS")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/ContactDetail/Business"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("HOME")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/ContactDetail/Home"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("MOBILE")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/ContactDetail/Mobile"),false));
					}
				}
									
				/* Insured Injury Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/injury[1]/description"),
							new XElement("InsuredDetails["+noOfInsured+"]/InsuredDriver/InjuryDetail"),false));
				
				/* Insured Vehicle Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle[1]/damage/pointOfImpact", "IMPACT_POINT"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/PointOfImpact"),OCAIConstants.APPEND_WITH_CODE_TO_DESCRIPTION));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle[1]/vehicleSpeed"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/VehicleSpeed"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle[1]/parkingPlace"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Location"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle[1]/vehicleRegistration[1]/externalReference"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/VehicleInfo/Registration"),false));
				//For Vehicle Make&Model
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle[1]/vehicleModelSpecification/make"),
							new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/VehicleInfo/MakeModel"), OCAIConstants.APPEND_WITHOUT_ALL));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle[1]/vehicleModelSpecification/model"),
							new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/VehicleInfo/MakeModel"), OCAIConstants.APPEND_WITHOUT_ALL));
							
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/repairActivity/excessAmount/theCurrencyAmount"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/RepairExcess"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/repairActivity/authorizationDate"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/AuthorisationDate"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/repairActivity/endDate"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/ActualCompletionDate"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/repairActivity/plannedEndDate"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/DueCompletionDate"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/repairActivity/startDate"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/InspectionDate"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/repairActivity/description"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/InspectionResult"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/repairActivity/estimatedRepairCost/theCurrencyAmount"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/RepairEstimation"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/underlyingAgreements[1]/issuingCompanyCode"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/PolicyIssueCompany"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/underlyingAgreements[1]/typeName", "INSURANCE_TYPE"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/PDInsuranceType", "INSURANCE_TYPE"),true));
			} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName")
						&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName").equalsIgnoreCase("CLAIMANT")) {
				/* Claimant General Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/prefixTitles"),
								new XElement("/ClaimantDriver/Title"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/firstName"),
								new XElement("/ClaimantDriver/FirstName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/lastName"),
								new XElement("/ClaimantDriver/LastName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/gender", "GENDER"),
								new XElement("/ClaimantDriver/Gender", "GENDER"),true));
	
				/* Claimant Address Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[1]"),
								new XElement("/ClaimantDriver/Address/AddressLine1"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[2]"),
								new XElement("/ClaimantDriver/Address/AddressLine2"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/city"),
								new XElement("/ClaimantDriver/Address/City"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/county"),
								new XElement("/ClaimantDriver/Address/County"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/country"),
								new XElement("/ClaimantDriver/Address/Country"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/postalCode"),
								new XElement("/ClaimantDriver/Address/PostCode"),false));
	
				/* Claimant Contact Details */
				partyContactDetails = allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber") != null? 
												allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber").size():OCAIConstants.ZERO;
				for(int contact=1;contact<=partyContactDetails;contact++) {
					if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("BUSINESS")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("/ClaimantDriver/ContactDetail/Business"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("HOME")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("/ClaimantDriver/ContactDetail/Home"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("MOBILE")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("/ClaimantDriver/ContactDetail/Mobile"),false));
					}
				}
				
				/* Claimant Injury Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/injury[1]/bodyPart", "BODY_PART"),
							new XElement("/ClaimantDriver/InjuryDetail"), OCAIConstants.APPEND_WITH_CODE_TO_DESCRIPTION));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/injury[1]/severity", "SEVERITY"),
							new XElement("/ClaimantDriver/InjuryDetail"), OCAIConstants.APPEND_WITH_CODE_TO_DESCRIPTION));
				/*transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/injury[1]/category"),
							new XElement("/ClaimantDriver/InjuryDetail"),OCAIConstants.APPEND_WITHOUT_ALL));*/
				
				/* Claimant Vehicle Details */
				thirdPartyVehicles = allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle") != null? 
										allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle").size():OCAIConstants.ZERO;
				for(int vehicle=1;vehicle<=thirdPartyVehicles;vehicle++) {
					transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle["+vehicle+"]/vehicleRegistration[1]/externalReference"),
									new XElement("/ClaimantVehicle["+vehicle+"]/VehicleInfo/Registration"),false));
					//For Vehicle Make&Model
					transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle["+vehicle+"]/vehicleModelSpecification/make"),
								new XElement("/ClaimantVehicle["+vehicle+"]/VehicleInfo/MakeModel"), OCAIConstants.APPEND_WITHOUT_ALL));
					transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle["+vehicle+"]/vehicleModelSpecification/model"),
								new XElement("/ClaimantVehicle["+vehicle+"]/VehicleInfo/MakeModel"), OCAIConstants.APPEND_WITHOUT_ALL));
					
					transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/iVehicle["+vehicle+"]/thirdPartyInsurance"),
									new XElement("/ClaimantVehicle["+vehicle+"]/ThirdPartyInsurance"),false));
				}
			} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName")
						&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName").equalsIgnoreCase("PASSENGER")) {
				passenger++;
				/* Passenger General Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/prefixTitles"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Title"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/firstName"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/FirstName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/lastName"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/LastName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/gender", "GENDER"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Gender", "GENDER"),true));
	
				/* Passenger Address Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[1]"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Address/AddressLine1"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[2]"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Address/AddressLine2"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/city"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Address/City"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/county"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Address/County"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/country"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Address/Country"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/postalCode"),
								new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/Address/PostCode"),false));
	
				/* Passenger Contact Details */
				partyContactDetails = allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber") != null? 
												allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber").size():OCAIConstants.ZERO;
				for(int contact=1;contact<=partyContactDetails;contact++) {
					if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("BUSINESS")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/ContactDetail/Business"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("HOME")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/ContactDetail/Home"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("MOBILE")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/ContactDetail/Mobile"),false));
					}
				}
				
				/* Passenger Injury Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/injury[1]/bodyPart", "BODY_PART"),
							new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/InjuryDetail"),OCAIConstants.APPEND_WITH_CODE_TO_DESCRIPTION));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/injury[1]/severity", "SEVERITY"),
							new XElement("InsuredDetails["+noOfInsured+"]/InsuredVehicle/Passenger["+passenger+"]/InjuryDetail"),OCAIConstants.APPEND_WITH_CODE_TO_DESCRIPTION));
				/*transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/injury[1]/category"),
							new XElement("/InsuredVehicle/Passenger["+passenger+"]/InjuryDetail"),OCAIConstants.APPEND_WITHOUT_ALL));*/
				
			} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName")
						&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/typeName").equalsIgnoreCase("WITNESS")) {
				witness++;
				/* Witness General Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/prefixTitles"),
								new XElement("/Witness["+witness+"]/PersonDetails/Title"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/firstName"),
								new XElement("/Witness["+witness+"]/PersonDetails/FirstName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/personName[1]/lastName"),
								new XElement("/Witness["+witness+"]/PersonDetails/LastName"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/gender", "GENDER"),
								new XElement("/Witness["+witness+"]/PersonDetails/Gender", "GENDER"),true));
	
				/* Witness Address Details */
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[1]"),
								new XElement("/Witness["+witness+"]/PersonDetails/Address/AddressLine1"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/addressLine[2]"),
								new XElement("/Witness["+witness+"]/PersonDetails/Address/AddressLine2"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/city"),
								new XElement("/Witness["+witness+"]/PersonDetails/Address/City"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/county"),
								new XElement("/Witness["+witness+"]/PersonDetails/Address/County"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/country"),
								new XElement("/Witness["+witness+"]/PersonDetails/Address/Country"),false));
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/postalAddress[1]/postalCode"),
								new XElement("/Witness["+witness+"]/PersonDetails/Address/PostCode"),false));
	
				/* Witness Contact Details */
				partyContactDetails = allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber") != null? 
												allocateSubrogationActivityRequest.getList("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber").size():OCAIConstants.ZERO;
				for(int contact=1;contact<=partyContactDetails;contact++) {
					if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("BUSINESS")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("/Witness["+witness+"]/PersonDetails/ContactDetail/Business"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("HOME")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("/Witness["+witness+"]/PersonDetails/ContactDetail/Home"),false));
					} else if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName")
							&& allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/typeName").equalsIgnoreCase("MOBILE")) {
						transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/party/telephoneNumber["+contact+"]/telephoneNumber"),
									new XElement("/Witness["+witness+"]/PersonDetails/ContactDetail/Mobile"),false));
					}
				}
				
				if(null != allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness")) {
					if(allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness").trim().equalsIgnoreCase(OCAIConstants.WITNESS_KNOW_TO_INSURED)) {
						allocateSubrogationActivityRequest.setString("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness", OCAIConstants.LEGIS_INSURED_PARTY);
					} else if(allocateSubrogationActivityRequest.getString("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness").trim().equalsIgnoreCase(OCAIConstants.WITNESS_KNOW_TO_THIRD_PARTY)) {
						allocateSubrogationActivityRequest.setString("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness", OCAIConstants.LEGIS_THIRD_PARTY);
					} else {
						allocateSubrogationActivityRequest.setString("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness", OCAIConstants.SPACE);
					}
				} else {
					allocateSubrogationActivityRequest.setString("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness", OCAIConstants.SPACE);
				}
				transList.add(new XTransMap(new XElement("subrogatedClaim/rolesInClaim["+partyNo+"]/knowsWitness"),
							new XElement("/Witness["+witness+"]/WitnessKnowTo"),false));
			}
		} 
	}
	
	/*CR121: Start Changes - Adding Services, Policy Effective Date & Policy Expiry Date*/
	if(null != allocateSubrogationActivityRequest.getDataObject("insurancePolicy")){
	
	services = allocateSubrogationActivityRequest.getString("insurancePolicy/divisionId");
	servicesOutput = transDAO.performCodeToDescriptionServices("SERVICES~"+services,
			OCAIConstants.ONECLAIM_SYSTEM, OCAIConstants.LEGIS_SYSTEM, OCAIConstants.COUNTRY_CODE_UK,OCAIConstants.LOB_CODE_AUTO, OCAIConstants.OCUK_CLAIM_SUBROGATION_REGION_CD,
			OCAIConstants.OCUK_CLAIM_SUBROGATION_BUSINESS_SEGMENT, OCAIConstants.OCUK_CLAIM_SUBROGATION_SUBLOB_CD,OCAIConstants.CLAIM_SUBROGATION_UK_LOG);
	System.out.println("Services Description :: "+ servicesOutput);
	if(servicesOutput!=null && servicesOutput.trim().length() > OCAIConstants.ZERO){
		servicesOutput = servicesOutput.trim();
		servicesOutputValue = servicesOutput.split("~")[OCAIConstants.TWO];
	}
			//transList.add(new XTransMap(new XElement("insurancePolicy/divisionId"), 
					//new XElement("/PolicyDetails/Services"),false));
			transList.add(new XTransMap(new XElement("insurancePolicy/startDate"), 
					new XElement("/PolicyDetails/PolicyEffectiveDate"),false));
			transList.add(new XTransMap(new XElement("insurancePolicy/plannedEndDate"), 
					new XElement("/PolicyDetails/PolicyExpiryDate"),false));
	}
	
	// For all code trasformations, translations and code to descriptions
	recordClaimDetailsRequestBO = xTransForm.transform(transList,allocateSubrogationActivityRequest,recordClaimDetailsRequestBO,OCAIConstants.ONECLAIM_SYSTEM,OCAIConstants.LEGIS_SYSTEM,
			OCAIConstants.COUNTRY_CODE_UK,OCAIConstants.LOB_CODE_AUTO, OCAIConstants.CLAIM_SUBROGATION_UK_LOG,
			OCAIConstants.OCUK_CLAIM_SUBROGATION_REGION_CD, OCAIConstants.OCUK_CLAIM_SUBROGATION_BUSINESS_SEGMENT, OCAIConstants.OCUK_CLAIM_SUBROGATION_SUBLOB_CD);
	recordClaimDetailsRequestBO.setString("PolicyDetails/Services", servicesOutputValue);
	/*CR121: Start Changes - Adding Insured Name, Insured Address, Contact Name & Broker Name*/
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/addressLine[1]", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/boxNumber", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/buildingName", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/city", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/country", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/county", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/floorNumber", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/houseNumber", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/postalCode", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/region", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/state", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/street", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/subregion", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/unitNumber", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/latitude", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/longitude", OCAIConstants.NOT_AVAILABLE );
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/postalBarcode", OCAIConstants.NOT_AVAILABLE );
	if(null !=  retrievePolicyDetailsResponse.getDataObject("insurancePolicies")){
			partyInvolvedInPolicy = retrievePolicyDetailsResponse.getList("insurancePolicies/partyInvolved")!=null? retrievePolicyDetailsResponse.getList("insurancePolicies/partyInvolved").size():OCAIConstants.ZERO;
			for(int partyNo=1;partyNo<=partyInvolvedInPolicy;partyNo++) {
				if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/typeName")
				&& retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/typeName").equalsIgnoreCase("INSURED")) {
					if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/lastName")){
						InsuredName = retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/lastName");
						if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/firstName")){
							contactName = retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/firstName") + retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/lastName");
						} else{
						contactName = retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/lastName");
						}
					}					
					noOfAddresses = retrievePolicyDetailsResponse.getList("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress")!=null? retrievePolicyDetailsResponse.getList("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress").size():OCAIConstants.ZERO;
					if(noOfAddresses > OCAIConstants.ZERO){
					insuredAddressFlag = OCAIConstants.ONE;
					}
					for(int addNo=1;addNo<=noOfAddresses;addNo++) {
						if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/typeName")
						&& retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/typeName").equalsIgnoreCase("Insured_Address")) {
							int noOfaddressLines = OCAIConstants.ZERO;
							noOfaddressLines = retrievePolicyDetailsResponse.getList("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/addressLine")!=null? retrievePolicyDetailsResponse.getList("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/addressLine").size():OCAIConstants.ZERO;
							for(int addressNo=1;addressNo<=noOfaddressLines;addressNo++) {
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/addressLine["+addressNo+"]", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/addressLine["+addressNo+"]"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/city")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/city", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/city"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/country")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/country", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/country"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/county")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/county", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/county"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/postalCode")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/postalCode", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/postalCode"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/boxNumber")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/boxNumber", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/boxNumber"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/buildingName")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/buildingName", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/buildingName"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/floorNumber")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/floorNumber", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/floorNumber"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/houseNumber")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/houseNumber", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/houseNumber"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/region")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/region", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/region"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/state")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/state", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/state"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/street")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/street", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/street"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/subregion")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/subregion", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/subregion"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/unitNumber")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/unitNumber", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/unitNumber"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/latitude")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/latitude", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/latitude"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/longitude")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/longitude", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/longitude"));
							}
							if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/postalBarcode")){
								recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredAddress/postalBarcode", retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/postalAddress["+addNo+"]/postalBarcode"));
							}
						}
					}
				}
				if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/typeName")
				&& retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/typeName").equalsIgnoreCase("Agent")) {
					if(null != retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/fullName")){
							BrokerName = retrievePolicyDetailsResponse.getString("insurancePolicies/partyInvolved["+partyNo+"]/party/personName[1]/fullName");
					}
				}				
			}
	}	

	if (contactName.equals(OCAIConstants.EMPTY_STRING)){
	contactName = OCAIConstants.NOT_AVAILABLE;
	}
	if (InsuredName.equals(OCAIConstants.EMPTY_STRING)){
	InsuredName = OCAIConstants.NOT_AVAILABLE;
	}
	if (BrokerName.equals(OCAIConstants.EMPTY_STRING)){
	BrokerName = OCAIConstants.NOT_AVAILABLE;
	}
	
	recordClaimDetailsRequestBO.setString("PolicyDetails/ContactName", contactName);
	recordClaimDetailsRequestBO.setString("PolicyDetails/InsuredName", InsuredName);
	recordClaimDetailsRequestBO.setString("PolicyDetails/BrokerName", BrokerName);
	
	/*CR121: End Changes - Adding Insured Name, Insured Address, Contact Name & Broker Name*/
	
      if(null != recordClaimDetailsRequestBO.getString("AccidentDetails/LossLocation")) {
            String lossEventLocation = recordClaimDetailsRequestBO.getString("AccidentDetails/LossLocation");
            lossLocation.append(lossEventLocation.split(", ")[OCAIConstants.ZERO]);
            lossLocation.append(", ");//for Road Type
            if(null != allocateSubrogationActivityRequest.getString("lossEvent/street")) {
                  lossLocation.append(allocateSubrogationActivityRequest.getString("lossEvent/street"));
            }
            lossLocation.append(", ");//for Street and house no
            if(null != allocateSubrogationActivityRequest.getString("lossEvent/city")) {
                  lossLocation.append(allocateSubrogationActivityRequest.getString("lossEvent/city"));
            }
            lossLocation.append(", ");//for city
            if(null != allocateSubrogationActivityRequest.getString("lossEvent/postalCode")) {
                  lossLocation.append(allocateSubrogationActivityRequest.getString("lossEvent/postalCode"));
            }
     } else {
            lossLocation.append(", ");//for Road Type
            if(null != allocateSubrogationActivityRequest.getString("lossEvent/street")) {
                  lossLocation.append(allocateSubrogationActivityRequest.getString("lossEvent/street"));
            }
            lossLocation.append(", ");//for Street and house no
            if(null != allocateSubrogationActivityRequest.getString("lossEvent/city")) {
                  lossLocation.append(allocateSubrogationActivityRequest.getString("lossEvent/city"));
            }
            lossLocation.append(", ");//for city
            if(null != allocateSubrogationActivityRequest.getString("lossEvent/postalCode")) {
                  lossLocation.append(allocateSubrogationActivityRequest.getString("lossEvent/postalCode"));
            }
     }
     recordClaimDetailsRequestBO.setString("AccidentDetails/LossLocation", lossLocation.toString());
	
	
	/** writing the request*/
	serializer.writeDataObject(recordClaimDetailsRequestBO, recordClaimDetailsRequestBO.getType().getURI(), recordClaimDetailsRequestBO.getType().getName(), stream);
	if(null != recordClaimDetailsRequestBO.getString("ClaimDetails/ClaimNo") && recordClaimDetailsRequestBO.getString("ClaimDetails/ClaimNo").trim().length() > OCAIConstants.ZERO) {
		legisOutbound.setString("one_claim_no", recordClaimDetailsRequestBO.getString("ClaimDetails/ClaimNo"));
	}
	if(targetSystemCode.trim().length() > OCAIConstants.ZERO) {
		legisOutbound.setString("target_system_cd", targetSystemCode);
	}
	if(notificationId > OCAIConstants.ZERO) {
		legisOutbound.setInt("notification_sqn", notificationId);
	}
	legisOutbound.setString("record_status_cd", OCAIConstants.NO);
	legisOutbound.setString("message_ob", stream.toString());
	
	legisOutboundRequest.setDataObject("KbondbaToutbound_Staging", legisOutbound);
	logger.log(Level.INFO, "Feed Type : " + targetSystemCode);
	logger.log(Level.INFO, String.format("Request After Transformation : "));
	logger.log(Level.INFO, stream.toString());
	logger.log(Level.INFO, "Prepare Subrogation Request Ends");
} catch (IOException io) {
	logger.log(Level.SEVERE, io.getMessage());
	bpelError = "Other";
} catch (Exception e) {
	if(e.getMessage().contains("Transformation exception")) {
		bpelError = "Transformation";
		errorInfo.setString("errorCode", "OCAI_UK_CS_BV_004");
		errorInfo.setString("errorMessageText", e.getMessage());
		errorInfo.setString("errorMessageType", "BUSINESS VALIDATION");
		errorInfo.setString("errorState", "WARNING");
	} else {
		bpelError = "Other";
	}
	logger.log(Level.SEVERE, e.getMessage());
}
