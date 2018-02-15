package com.chartis.xtrans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chartis.gci.utility.GCILogger;
import com.chartis.gci.utility.OCAIConstants;
import commonj.sdo.DataObject;

/**
 * XTransform holds the logic to perform the Message Transformation.
 * 
 * @author
 * 
 */
public class XTransform {
	public ArrayList transformations = new ArrayList<XTransMap>();
	public ArrayList codeTranslateLookUps = new ArrayList<XTransMap>();
	public ArrayList codeDescriptionLookUps = new ArrayList<XTransMap>();
	public commonj.sdo.DataObject transformdSdo = null;
	// private Logger logger = new GCILogger().getGCILogger("XTransformation");
	private Logger logger = null;

	private String callString;

	public String getCallString() {
		return callString;
	}

	public void setCallString(String callString) {
		this.callString = callString;
	}

	/**
	 * RDF lookup, this is a depreciated method after Release 4
	 * 
	 * @deprecated method
	 * @param trans
	 * @return
	 * @throws Exception
	 */
	public commonj.sdo.DataObject transform(ArrayList trans,
			commonj.sdo.DataObject sourceDO, commonj.sdo.DataObject targetDO,
			String sourceSystem, String targetSystem, String country,
			String lob, String moduleName) throws Exception {

		Logger logger = new GCILogger().getGCILogger("XTransformation",
				moduleName);
		transformations = trans;
		boolean toLookup = false;
		for (Object obj : transformations) {
			XTransMap xTMap = (XTransMap) obj;
			XElement sourceXE = (XElement) xTMap.getSourceElement();
			XElement targetXE = (XElement) xTMap.getTargetElement();
			/* set value from Source to Target */
			if (isValidPath(sourceDO, sourceXE.getXPath())) {
				/*logger.log(Level.INFO, "Accessing (" + sourceXE.getXPath()
						+ ") from " + sourceDO.getType().toString()
						+ ". Value is :" + sourceDO.get(sourceXE.getXPath())); */
				
					//Added by vijay to fix 0E 8 issue with Big decimals
				
				if (sourceDO.get(sourceXE.getXPath()) instanceof  java.math.BigDecimal ){
					java.math.BigDecimal dec = new BigDecimal("0.0");
					try{			
						dec=(BigDecimal)sourceDO.get(sourceXE.getXPath());
						targetDO.set(targetXE.getXPath(), dec.toPlainString());	
					
					}catch(Exception e){
						targetDO.set(targetXE.getXPath(), sourceDO.get(sourceXE.getXPath()));
					   }
					}	
				     else{	
							targetDO.set(targetXE.getXPath(), sourceDO.get(sourceXE.getXPath()));
				         }
				
				
			}
			/* Add to codeLookUps list */
			if (xTMap.isToTranslate()
					&& sourceDO.get(sourceXE.getXPath()) != null) {
				if (sourceDO.get(sourceXE.getXPath()) instanceof java.lang.String) {
					String srcTmp = (String) sourceDO.get(sourceXE.getXPath());
					if (!srcTmp.trim().equalsIgnoreCase("")) {
						codeTranslateLookUps.add(xTMap);
						toLookup = true;
					}
				}
			}
		}
		transformdSdo = targetDO;
		logger.log(Level.INFO, "Message translation is done.");
		/*
		 * When finished with all Message Transformations (Mappings), perform
		 * Code Look up Values to be transformed will be read from source and
		 * the targetSDO will be set with the looked up values.
		 */
		if (toLookup) {
			CodeLookUp clp = new CodeLookUp();
			logger.log(Level.INFO, "Looking up for code transformations.");
			transformdSdo = clp.lookUpAndUpdate(codeTranslateLookUps, sourceDO,
					transformdSdo, sourceSystem, targetSystem, country, lob,
					moduleName);
		}
		logger.log(Level.INFO, "Returning Transformed and translated SDO.");
		return transformdSdo;
	}

	/**
	 * Extended from existing code, has been created by Antony for CR 111430.
	 * RDF lookup, This method accepts list of RDF parameters and return
	 * transformed return code. Includes region code, product line and Lob sub
	 * code
	 * 
	 * @param trans
	 * @return
	 * @throws Exception
	 */
	public commonj.sdo.DataObject transform(ArrayList trans,
			commonj.sdo.DataObject sourceDO, commonj.sdo.DataObject targetDO,
			String sourceSystem, String targetSystem, String country,
			String lob, String moduleName, String regionCd,
			String productLineCd, String lobSubCd) throws Exception {

		Logger logger = new GCILogger().getGCILogger(moduleName, moduleName);
		codeTranslateLookUps = new ArrayList<XTransMap>();
		transformations = trans;
		boolean toLookup = false;
		boolean codetoDesLookup = false;
		try
		{
				for (Object obj : transformations) {
				XTransMap xTMap = (XTransMap) obj;
				XElement sourceXE = (XElement) xTMap.getSourceElement();
				XElement targetXE = (XElement) xTMap.getTargetElement();
				/* set value from Source to Target */
				if (isValidPath(sourceDO, sourceXE.getXPath())) {
					/*logger.log(Level.INFO, "Accessing (" + sourceXE.getXPath()
							+ ") from " + sourceDO.getType().toString()
							+ ". Value is :" + sourceDO.get(sourceXE.getXPath())); */
					targetDO.set(targetXE.getXPath(), sourceDO.get(sourceXE
							.getXPath()));
				}
				
					/* Add to codeLookUps list */
				if (xTMap.isToTranslate() 
						&& isValidPath(sourceDO, sourceXE.getXPath())
						&& sourceDO.get(sourceXE.getXPath()) != null) {
					if (sourceDO.get(sourceXE.getXPath()) instanceof java.lang.String) {
					String srcTmp = (String) sourceDO.get(sourceXE.getXPath());
						if (!srcTmp.trim().equalsIgnoreCase("")) {
						codeTranslateLookUps.add(xTMap);
							toLookup = true;
						}
					}
				}
				
				if (xTMap.isCodeToDescription()) {

					String str = (String) sourceDO.get(sourceXE.getXPath());
					if (str != null && !str.trim().equalsIgnoreCase("")) {
						System.out.println("Inside Code2Desc");

						codeDescriptionLookUps.add(xTMap);
						codetoDesLookup = true;
					}

					if (xTMap.getToAppend().equalsIgnoreCase("Append")) {

						targetDO.set(targetXE.getXPath(), null);
					}
				}
							}
			transformdSdo = targetDO;
			logger.log(Level.INFO, "Message translation is done.");
		}
		catch(Exception ex){
						ex.printStackTrace();
		}
		/*
		 * When finished with all Message Transformations (Mappings), perform
		 * Code Look up Values to be transformed will be read from source and
		 * the targetSDO will be set with the looked up values.
		 */
		 try{
				if (toLookup) {
			CodeLookUp clp = new CodeLookUp();
			logger.log(Level.INFO, "Looking up for code transformations.");
			transformdSdo = clp.lookUpAndUpdate(codeTranslateLookUps, sourceDO,
					transformdSdo, sourceSystem, targetSystem, country, lob,
					moduleName, regionCd, productLineCd, lobSubCd);
					
			setCallString(clp.getCallString());
		}
		if (codetoDesLookup) {

			CodeLookUp clp = new CodeLookUp();
			logger.log(Level.INFO, "Looking up for code to descriptions.");
			transformdSdo = clp
					.codeToDescription(codeDescriptionLookUps, sourceDO,
							transformdSdo, sourceSystem, targetSystem, country,
							lob, moduleName, regionCd, productLineCd, lobSubCd);
							
			setCallString(clp.getCallString());
		}
			} catch (Exception e) {
			// TODO: handle exception
			
			throw new Exception(e);
			}
			
		logger.log(Level.INFO, "Returning Transformed and translated SDO.");
		return transformdSdo;
	}

	/**
	 * Checks if message has valid xPath
	 * 
	 * @param bo
	 * @param xPath
	 * @return
	 */
	public boolean isValidPath(DataObject bo, String xPath) {
		try {

			// logger.log(Level.INFO, "Entered isValidPath Method");
			bo.get(xPath);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public commonj.sdo.DataObject transform_r3(ArrayList trans,
			commonj.sdo.DataObject sourceDO, commonj.sdo.DataObject targetDO,
			String sourceSystem, String targetSystem, String country,
			String lob, String moduleName) throws Exception {
		logger = GCILogger.getGCILogger("XTransformation", moduleName);
		transformations = trans;

		boolean toLookup = false;
		boolean codetoDesLookup = false;

		for (Object obj : transformations) {

			XTransMap xTMap = (XTransMap) obj;

			XElement sourceXE = (XElement) xTMap.getSourceElement();

			XElement targetXE = (XElement) xTMap.getTargetElement();

			/* set value from Source to Target */
			// Commented on 06/18/2010-->logger.log(Level.INFO,
			// "In setting the path :" + sourceXE.getXPath());
			if (isValidPath(sourceDO, sourceXE.getXPath())
					&& (sourceXE.getLookUpString() != null && !sourceXE
							.getLookUpString().contains("CAT_CODE"))
			/*
			 * CATASTRPOHIC CODE TO BE AVOIDED SINCE THE TARGET TAG WILL BE
			 * CHOSEN DYNAMICALLY AFTER SP CALL
			 */
			) {
					if (sourceDO.get(sourceXE.getXPath()) instanceof  java.math.BigDecimal ){
		java.math.BigDecimal dec = new BigDecimal("0.0");
		try{			
			dec=(BigDecimal)sourceDO.get(sourceXE.getXPath());
			targetDO.set(targetXE.getXPath(), dec.toPlainString());	
		
		}catch(Exception e){
			targetDO.set(targetXE.getXPath(), sourceDO.get(sourceXE.getXPath()));
		   }
		}	
	     else{	
				targetDO.set(targetXE.getXPath(), sourceDO.get(sourceXE.getXPath()));
	         }
				
			}

			/* Add to codeLookUps list */
			if (xTMap.isToTranslate()) {
				if (sourceDO.get(sourceXE.getXPath()) instanceof java.lang.String) {
					String str = (String) sourceDO.get(sourceXE.getXPath());
					if (str != null && !str.trim().equalsIgnoreCase("")) {
						codeTranslateLookUps.add(xTMap);
						toLookup = true;
					}
				}
			}

			if (xTMap.isCodeToDescription()) {
				String str = (String) sourceDO.get(sourceXE.getXPath());
				if (str != null && !str.trim().equalsIgnoreCase("")) {
					codeDescriptionLookUps.add(xTMap);
					codetoDesLookup = true;
				}

				if (xTMap.getToAppend().equalsIgnoreCase("Append")) {
					targetDO.set(targetXE.getXPath(), null);
				}
			}

		}

		transformdSdo = targetDO;
		logger.log(Level.INFO, "Message translation is done.**");
		/*
		 * When finished with all Message Transformations (Mappings), perform
		 * Code Look up Values to be transformed will be read from source and
		 * the targetSDO will be set with the looked up values.
		 */
		if (toLookup) {
			CodeLookUp clp = new CodeLookUp();
			logger.log(Level.INFO, "Looking up for code transformations.");

			transformdSdo = clp.lookUpAndUpdate_r3(codeTranslateLookUps,
					sourceDO, transformdSdo, sourceSystem, targetSystem,
					country, lob, moduleName);

		}
		if (codetoDesLookup) {
			CodeLookUp clp = new CodeLookUp();
			logger.log(Level.INFO, "Looking up for code to descriptions.");
			// transformdSdo =
			// clp.codeToDescription(codeDescriptionLookUps,sourceDO,transformdSdo,sourceSystem,targetSystem,country,lob,moduleName);
		}
		// for(Object obj:transformations){
		// XTransMap xTMap = (XTransMap)obj;
		// XElement sourceXE = (XElement) xTMap.getSourceElement();
		// XElement targetXE = (XElement) xTMap.getTargetElement();
		// /* set value from Source to Target*/
		// if(isValidPath(sourceDO, sourceXE.getXPath())){
		// if(!xTMap.isToTranslate() && !xTMap.isCodeToDescription()) {
		// transformdSdo.set(targetXE.getXPath(),
		// sourceDO.get(sourceXE.getXPath()));
		// }
		// }
		// }
		logger.log(Level.INFO, "Message translation is done.");
		logger.log(Level.INFO, "Returning Transformed and translated SDO.");

		return transformdSdo;
	}

	/**
	 * Merged by Antony, Copied from UKILoss notification library
	 * 
	 * @param bo
	 * @param xPath
	 * @return
	 */
	public boolean isValidIntPath(DataObject bo, String xPath) {
		// if(logger == null){ // External call
		// logger = new GCILogger().getGCILogger("XTransformation",moduleName);
		// }
		try {
			bo.getInt(xPath);
		} catch (Exception e) {
			// logger.log(Level.SEVERE,
			// "Failed to find ("+xPath+") in Message : "+bo.getType().getName());
			return false;
		}
		return true;
	}

	public commonj.sdo.DataObject transform_r3(ArrayList trans,
			commonj.sdo.DataObject sourceDO, commonj.sdo.DataObject targetDO,
			String sourceSystem, String targetSystem, String country,
			String lob, String moduleName, String regionCd,
			String productLineCd, String lobSubCd) throws Exception {

		//logger = GCILogger.getGCILogger("XTransformation", moduleName);
		logger = GCILogger.getNewGCILogger("XTransformation", moduleName, country); //will call the new logging method
		transformations = trans;
		codeTranslateLookUps = new ArrayList<XTransMap>();
		boolean toLookup = false;
		boolean codetoDesLookup = false;

		for (Object obj : transformations) {
			XTransMap xTMap = (XTransMap) obj;
			XElement sourceXE = (XElement) xTMap.getSourceElement();
			XElement targetXE = (XElement) xTMap.getTargetElement();
			/* set value from Source to Target */
			// Commented on 06/18/2010-->logger.log(Level.INFO,
			// "In setting the path :" + sourceXE.getXPath());
			if (isValidPath(sourceDO, sourceXE.getXPath())
					&& (sourceXE.getLookUpString() != null && !sourceXE
							.getLookUpString().contains("CAT_CODE"))) {
				/*
				 * CATASTRPOHIC CODE TO BE AVOIDED SINCE THE TARGET TAG WILL BE
				 * CHOSEN DYNAMICALLY AFTER SP CALL
				 */
				targetDO.set(targetXE.getXPath(), sourceDO.get(sourceXE
						.getXPath()));
			}

			/* Add to codeLookUps list */
			if (xTMap.isToTranslate()
					&& (isValidPath(sourceDO, sourceXE.getXPath()))
					&& sourceDO.get(sourceXE.getXPath()) != null) {
				if (sourceDO.get(sourceXE.getXPath()) instanceof java.lang.String) {
					String str = (String) sourceDO.get(sourceXE.getXPath());
					if (str != null && !str.trim().equalsIgnoreCase("")) {
						codeTranslateLookUps.add(xTMap);
						toLookup = true;
					}
				}
			}
			if (xTMap.isCodeToDescription()) {
				String str = (String) sourceDO.get(sourceXE.getXPath());
				if (str != null && !str.trim().equalsIgnoreCase("")) {
					codeDescriptionLookUps.add(xTMap);
					codetoDesLookup = true;
				}
				if (xTMap.getToAppend().equalsIgnoreCase("Append")) {
					targetDO.set(targetXE.getXPath(), null);
				}
			}

		}

		transformdSdo = targetDO;
		logger.log(Level.INFO, "Message translation is done.**");
		/*
		 * When finished with all Message Transformations (Mappings), perform
		 * Code Look up Values to be transformed will be read from source and
		 * the targetSDO will be set with the looked up values.
		 */
		if (toLookup) {
			CodeLookUp clp = new CodeLookUp();
			logger.log(Level.INFO, "Looking up for code transformations.");

			transformdSdo = clp
					.lookUpAndUpdate_r3(codeTranslateLookUps, sourceDO,
							transformdSdo, sourceSystem, targetSystem, country,
							lob, moduleName, regionCd, productLineCd, lobSubCd);

		}
		if (codetoDesLookup) {
			CodeLookUp clp = new CodeLookUp();
			logger.log(Level.INFO, "Looking up for code to descriptions.");
			// transformdSdo =
			// clp.codeToDescription(codeDescriptionLookUps,sourceDO,transformdSdo,sourceSystem,targetSystem,country,lob,moduleName);
		}
		// for(Object obj:transformations){
		// XTransMap xTMap = (XTransMap)obj;
		// XElement sourceXE = (XElement) xTMap.getSourceElement();
		// XElement targetXE = (XElement) xTMap.getTargetElement();
		// /* set value from Source to Target*/
		// if(isValidPath(sourceDO, sourceXE.getXPath())){
		// if(!xTMap.isToTranslate() && !xTMap.isCodeToDescription()) {
		// transformdSdo.set(targetXE.getXPath(),
		// sourceDO.get(sourceXE.getXPath()));
		// }
		// }
		// }
		logger.log(Level.INFO, "Message translation is done.");
		logger.log(Level.INFO, "Returning Transformed and translated SDO.");

		return transformdSdo;
	}

	/**
	 * Checks if message has valid xPath and whether the value is XPATH is NULL
	 * or not
	 * 
	 * @param bo
	 * @param xPath
	 * @return
	 */
	public boolean isValidPathAndNotNull(DataObject bo, String xPath) {
		try {
			if (null == bo.get(xPath) || null == bo.getString(xPath)
					|| bo.getString(xPath).isEmpty()) {
				return false;
			}

		} catch (Exception e) {
			return false;
		}
		return true;
	}

//Start Copied below Methods from Europe\Build\COUNTRY\2_2_0_ZA\Deployment\OCEU_GIFILossNotification_v2_2_0App

	//Added as part of CUEPI for EMEA Upgrade
	
	/**
	 * Checks for null or empty
	 * @param data String
	 * @return
	 */
	public boolean isValidData(String data) {
		if(null != data && !data.trim().equalsIgnoreCase("")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @param request
	 * @param response
	 * @param srcXpath
	 * @param trgXpath
	 */
	public void transformClaimStatus(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String claimStatus = request.getString(srcXpath);
		if(OCAIConstants.Claim_Status_O1.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_OPEN);
		} else if(OCAIConstants.Claim_Status_O4.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_OPEN);
		} else if(OCAIConstants.Claim_Status_S1.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_CLOSED);
		} else if(OCAIConstants.Claim_Status_W1.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_CLOSED);
		} else if(OCAIConstants.Claim_Status_W2.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_CLOSED);
		} else if(OCAIConstants.Claim_Status_W3.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_CLOSED);
		} else if(OCAIConstants.Claim_Status_W4.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_CLOSED);
		} else if(OCAIConstants.Claim_Status_W5.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_CLOSED);
		} else if(OCAIConstants.Claim_Status_W6.equalsIgnoreCase(claimStatus)) {
			response.set(trgXpath, OCAIConstants.Claim_Status_CLOSED);
		} else {
			response.set(trgXpath, claimStatus);
		}
	}
	
	/**
	 * @param request
	 * @param response
	 * @param srcXpath
	 * @param trgXpath
	 */
	public void transformNCDFlag(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String ncdFlag = request.getString(srcXpath);
		if(OCAIConstants.NCD_Flag_D.equalsIgnoreCase(ncdFlag)) {
			response.set(trgXpath, OCAIConstants.NCD_Flag_Disallowed);
		} else if(OCAIConstants.NCD_Flag_A.equalsIgnoreCase(ncdFlag)) {
			response.set(trgXpath, OCAIConstants.NCD_Flag_Allowed);
		} else if(OCAIConstants.NCD_Flag_U.equalsIgnoreCase(ncdFlag)) {
			response.set(trgXpath, OCAIConstants.NCD_Flag_Unrecorded);
		} else if(OCAIConstants.NCD_Flag_N.equalsIgnoreCase(ncdFlag)) {
			response.set(trgXpath, OCAIConstants.NCD_Flag_NotApplicable);
		}
	}
	//temp cover type
	public void transformCoverType(DataObject moAddBodyResponseType, DataObject cueMatchService, String srcXpath, String trgXpath) {
		if("A".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_A);
	    } else if("B".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_B);
	    } else if("C".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_C);
	    } else if("D".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_D);
	    } else if("E".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_E);
	    } else if("F".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_F);
	    } else if("G".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_G);
	    } else if("H".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_H);
	    } else if("I".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_I);
	    } else if("J".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
	          cueMatchService.set(trgXpath, OCAIConstants.Cover_Type_J);
	    } else {
	    	cueMatchService.set(trgXpath, moAddBodyResponseType.getString(srcXpath));
	    }
	}  
	//temp cover type  
  
	//damage status
	public void transformDamageStatus(DataObject moAddBodyResponseType, DataObject cueMatchService, String srcXpath, String trgXpath) {
		if("A".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
			cueMatchService.set(trgXpath, OCAIConstants.Damage_Status_A);
    	} else if("B".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.Damage_Status_B);
    	} else if("C".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.Damage_Status_C);
    	} else if("D".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
    		cueMatchService.set(trgXpath, OCAIConstants.Damage_Status_D);
    	} else if("T".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
    		cueMatchService.set(trgXpath, OCAIConstants.Damage_Status_T);
    	} else if("F".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.Damage_Status_F);
    	} else if("O".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
    		cueMatchService.set(trgXpath, OCAIConstants.Damage_Status_O);
    	} else {
	    	cueMatchService.set(trgXpath, moAddBodyResponseType.getString(srcXpath));
	    }		
	}
	//damage status
	
	public void transformRecoveredStatus(DataObject moAddBodyResponseType, DataObject cueMatchService, String srcXpath, String trgXpath) {
		if("N".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
			cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_N);
    	} else if("Y".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_Y);
    	} else if("A".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_A);
    	} else if("B".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
    		cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_B);
    	} else if("C".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
    		cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_C);
    	} else if("D".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_D);
    	} else if("F".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
    		cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_F);
    	} else if("O".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
    		cueMatchService.set(trgXpath, OCAIConstants.Rec_Status_O);
    	} else {
	    	cueMatchService.set(trgXpath, moAddBodyResponseType.getString(srcXpath));
	    }		
	}
	
	public void transformRegnInd(DataObject moAddBodyResponseType, DataObject cueMatchService, String srcXpath, String trgXpath) {
		if("N".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
			cueMatchService.set(trgXpath, OCAIConstants.RegnInd_Status_N);
    	} else if("F".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.RegnInd_Status_F);
    	} else if("U".equalsIgnoreCase(moAddBodyResponseType.getString(srcXpath))) {
          	cueMatchService.set(trgXpath, OCAIConstants.RegnInd_Status_U);
    	} else {
	    	cueMatchService.set(trgXpath, moAddBodyResponseType.getString(srcXpath));
	    }		
	}
	public void transformCollective(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String collective = request.getString(srcXpath);
		if(collective.equalsIgnoreCase("true")) {
			response.set(trgXpath, OCAIConstants.PolicyLeader);
		} else if(collective.equalsIgnoreCase("false")) {
			response.set(trgXpath, OCAIConstants.NonCollectivePolicy);
		} 
	}
	public void transformLossCause(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String lossCause = request.getString(srcXpath);
		if("10".equalsIgnoreCase(lossCause)) {
			response.set(trgXpath, OCAIConstants.LossCause_Accident);
		} else if("20".equalsIgnoreCase(lossCause)) {
			response.set(trgXpath, OCAIConstants.LossCause_Fire);
		} else if("30".equalsIgnoreCase(lossCause)) {
			response.set(trgXpath, OCAIConstants.LossCause_Theft_30);
		} else if("31".equalsIgnoreCase(lossCause)) {
			response.set(trgXpath, OCAIConstants.LossCause_Theft_31);
		} else if("32".equalsIgnoreCase(lossCause)) {
			response.set(trgXpath, OCAIConstants.LossCause_Theft_32);
		} else if("40".equalsIgnoreCase(lossCause)) {
			response.set(trgXpath, OCAIConstants.LossCause_Other);
		} else if("50".equalsIgnoreCase(lossCause)) {
			response.set(trgXpath, OCAIConstants.LossCause_NotKnown);
		} else {
			response.set(trgXpath, lossCause);
		}
	}
	public void transformAddressIndicator(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String addressInd = request.getString(srcXpath);
		if("P".equalsIgnoreCase(addressInd)) {
			response.set(trgXpath, OCAIConstants.ADDRESS_INDICATOR_PAF_VALID);
		} else if("F".equalsIgnoreCase(addressInd)) {
			response.set(trgXpath, OCAIConstants.ADDRESS_INDICATOR_FOREIGN);
		} else {//	"A".equalsIgnoreCase(addressInd)
			response.set(trgXpath, OCAIConstants.ADDRESS_INDICATOR_AS_INPUT);
		} 
	}
	public void transformPolicyType(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String policyType = request.getString(srcXpath);
		if("E".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_E);
		} else if("G".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_G);
		} else if("H".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_H);
		}else if("L".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_L);
		}else if("M".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_M);
		}else if("N".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_N);
		}else if("P".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_P);
		}else if("R".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_R);
		}else if("T".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_T);
		}else if("X".equalsIgnoreCase(policyType)) {
			response.set(trgXpath, OCAIConstants.POLICY_TYPE_X);
		}
	}
	
	/**
	 * @param request
	 * @param response
	 * @param srcXpath
	 * @param trgXpath
	 */
	public void transformTitle(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String title = request.getString(srcXpath);
		if("0".equalsIgnoreCase(title)) {
			response.set(trgXpath, OCAIConstants.TITLE_0);
		} else if("1".equalsIgnoreCase(title)) {
			response.set(trgXpath, OCAIConstants.TITLE_1);
		} else if("2".equalsIgnoreCase(title)) {
			response.set(trgXpath, OCAIConstants.TITLE_2);
		} else if("3".equalsIgnoreCase(title)) {
			response.set(trgXpath, OCAIConstants.TITLE_3);
		} else if("4".equalsIgnoreCase(title)) {
			response.set(trgXpath, OCAIConstants.TITLE_4);
		} else if("5".equalsIgnoreCase(title)) {
			response.set(trgXpath, OCAIConstants.TITLE_5);
		} else if("6".equalsIgnoreCase(title)) {
			response.set(trgXpath, OCAIConstants.TITLE_6);
		} else {
			response.set(trgXpath, title);
		}
	}
	
	/**
	 * @param request DataObject
	 * @param response
	 * @param srcXpath
	 * @param trgXpath
	 */
	public void transformSubjectCode(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String subjectCode = request.getString(srcXpath);
		if("D1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_D1);
		} else if("E1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_E1);
		} else	if("C1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_C1);
		} else	if("B1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_B1);
		} else if("D2".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_D2);
		} else if("E2".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_E2);
		} else if("F1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_F1);
		} else if("A1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_A1); 
		} else if("P1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_P1); 
		}else if("Q1".equalsIgnoreCase(subjectCode)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_Q1); 
		} else {
			response.set(trgXpath, subjectCode);
		}
	}
	
	public void transformSubjectRole(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String subjectRole = request.getString(srcXpath);
		if("PH".equalsIgnoreCase(subjectRole)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_PH);
		} else if("DR".equalsIgnoreCase(subjectRole)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_DR);
		} else	if("TP".equalsIgnoreCase(subjectRole)) {
			response.set(trgXpath, OCAIConstants.SUBJECT_CODE_TP);
		} else {
			response.set(trgXpath, subjectRole);
		}
	}
	
	/**
	 * @param request
	 * @param response
	 * @param srcXpath
	 * @param trgXpath
	 */
	public void transformSex(DataObject request, DataObject response, String srcXpath, String trgXpath) {
		String sexCode = request.getString(srcXpath);
		if("M".equalsIgnoreCase(sexCode)) {
			response.set(trgXpath, OCAIConstants.SEX_MALE);
		} else if("F".equalsIgnoreCase(sexCode)) {
			response.set(trgXpath, OCAIConstants.SEX_FEMALE);
		} else	if("N".equalsIgnoreCase(sexCode)) {
			response.set(trgXpath, OCAIConstants.SEX_NOT_KNOWN);
		} else {
			response.set(trgXpath, OCAIConstants.SEX_NOT_KNOWN);
		}
	}
	
	/**
	 * @param sex
	 * @return
	 */
	public String transformSexRequest(String sex) {
		if(OCAIConstants.SEX_MALE.equalsIgnoreCase(sex)) {
			return OCAIConstants.SEX_M;
		} else if(OCAIConstants.SEX_FEMALE.equalsIgnoreCase(sex)) {
			return OCAIConstants.SEX_F;
		} else	if(OCAIConstants.SEX_NOT_KNOWN.equalsIgnoreCase(sex)) {
			return OCAIConstants.SEX_N;
		} else	if(OCAIConstants.SEX_UN_KNOWN.equalsIgnoreCase(sex)) {
			return OCAIConstants.SEX_N;
		} else {
			return OCAIConstants.SEX_N;
		}
	}
	
	
	//Added as part of CUEPI for EMEA Upgrade

//End Copied below Methods from Europe\Build\COUNTRY\2_2_0_ZA\Deployment\OCEU_GIFILossNotification_v2_2_0App
}
