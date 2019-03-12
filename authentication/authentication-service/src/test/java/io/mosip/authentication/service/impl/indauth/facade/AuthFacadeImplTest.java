package io.mosip.authentication.service.impl.indauth.facade;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.authentication.core.dto.indauth.AdditionalFactorsDTO;
import io.mosip.authentication.core.dto.indauth.AuthRequestDTO;
import io.mosip.authentication.core.dto.indauth.AuthResponseDTO;
import io.mosip.authentication.core.dto.indauth.AuthStatusInfo;
import io.mosip.authentication.core.dto.indauth.AuthTypeDTO;
import io.mosip.authentication.core.dto.indauth.BioIdentityInfoDTO;
import io.mosip.authentication.core.dto.indauth.BioInfo;
import io.mosip.authentication.core.dto.indauth.IdType;
import io.mosip.authentication.core.dto.indauth.IdentityDTO;
import io.mosip.authentication.core.dto.indauth.IdentityInfoDTO;
import io.mosip.authentication.core.dto.indauth.KycAuthRequestDTO;
import io.mosip.authentication.core.dto.indauth.KycAuthResponseDTO;
import io.mosip.authentication.core.dto.indauth.KycMetadataDTO;
import io.mosip.authentication.core.dto.indauth.KycResponseDTO;
import io.mosip.authentication.core.dto.indauth.RequestDTO;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.exception.IdAuthenticationDaoException;
import io.mosip.authentication.core.spi.id.service.IdAuthService;
import io.mosip.authentication.core.spi.id.service.IdRepoService;
import io.mosip.authentication.core.spi.indauth.service.BioAuthService;
import io.mosip.authentication.core.spi.indauth.service.DemoAuthService;
import io.mosip.authentication.core.spi.indauth.service.KycService;
import io.mosip.authentication.core.spi.indauth.service.OTPAuthService;
import io.mosip.authentication.core.spi.indauth.service.PinAuthService;
import io.mosip.authentication.service.config.IDAMappingConfig;
import io.mosip.authentication.service.entity.AutnTxn;
import io.mosip.authentication.service.factory.RestRequestFactory;
import io.mosip.authentication.service.helper.AuditHelper;
import io.mosip.authentication.service.helper.IdInfoHelper;
import io.mosip.authentication.service.helper.RestHelper;
import io.mosip.authentication.service.impl.indauth.builder.AuthStatusInfoBuilder;
import io.mosip.authentication.service.impl.indauth.service.KycServiceImpl;
import io.mosip.authentication.service.impl.indauth.service.bio.BioAuthType;
import io.mosip.authentication.service.impl.indauth.service.demo.DemoMatchType;
import io.mosip.authentication.service.impl.notification.service.NotificationServiceImpl;
import io.mosip.authentication.service.integration.IdTemplateManager;
import io.mosip.authentication.service.integration.NotificationManager;
import io.mosip.authentication.service.integration.OTPManager;
import io.mosip.authentication.service.repository.AutnTxnRepository;
import io.mosip.kernel.core.idgenerator.spi.TokenIdGenerator;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
import io.mosip.kernel.templatemanager.velocity.builder.TemplateManagerBuilderImpl;

/**
 * The class validates AuthFacadeImpl.
 *
 * @author Arun Bose
 * 
 * 
 * @author Prem Kumar
 */
@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class, TemplateManagerBuilderImpl.class })

public class AuthFacadeImplTest {

	private static final String STATUS_SUCCESS = "Y";
	/** The auth facade impl. */
	@InjectMocks
	private AuthFacadeImpl authFacadeImpl;
	@Mock
	private AuthFacadeImpl authFacadeMock;
	/** The env. */
	@Autowired
	private Environment env;

	@InjectMocks
	private KycServiceImpl kycServiceImpl;

	/** The otp auth service impl. */
	@Mock
	private OTPAuthService otpAuthServiceImpl;
	/** The IdAuthService */
	@Mock
	private IdAuthService<AutnTxn> idAuthService;
	/** The KycService **/
	@Mock
	private KycService kycService;
	/** The IdInfoHelper **/
	@Mock
	private IdInfoHelper idInfoHelper;
	/** The IdRepoService **/
	@Mock
	private IdAuthService idInfoService;
	/** The DemoAuthService **/
	@Mock
	private DemoAuthService demoAuthService;

	@Autowired
	private TemplateManagerBuilder templateManagerBuilder;

	@Mock
	private IDAMappingConfig idMappingConfig;

	@InjectMocks
	NotificationServiceImpl notificationService;

	@Mock
	NotificationManager notificationManager;

	@Mock
	private IdTemplateManager idTemplateManager;

	@InjectMocks
	private RestRequestFactory restRequestFactory;

	@InjectMocks
	private RestHelper restHelper;

	@InjectMocks
	private OTPManager otpManager;

	@Mock
	private BioAuthService bioAuthService;
	@Mock
	private AuditHelper auditHelper;
	@Mock
	private IdRepoService idRepoService;
	@Mock
	private AutnTxnRepository autntxnrepository;

	@Mock
	private PinAuthService pinAuthService;

	@Mock
	private TokenIdGenerator<String, String> tokenIdGenerator;

	/**
	 * Before.
	 */
	@Before
	public void before() {
		ReflectionTestUtils.setField(authFacadeImpl, "otpService", otpAuthServiceImpl);
		ReflectionTestUtils.setField(authFacadeImpl, "tokenIdGenerator", tokenIdGenerator);
		ReflectionTestUtils.setField(authFacadeImpl, "pinAuthService", pinAuthService);
		ReflectionTestUtils.setField(authFacadeImpl, "kycService", kycService);
		ReflectionTestUtils.setField(authFacadeImpl, "bioAuthService", bioAuthService);
		ReflectionTestUtils.setField(authFacadeImpl, "auditHelper", auditHelper);
		ReflectionTestUtils.setField(authFacadeImpl, "env", env);
		ReflectionTestUtils.setField(restRequestFactory, "env", env);

		ReflectionTestUtils.setField(kycServiceImpl, "idInfoHelper", idInfoHelper);
		ReflectionTestUtils.setField(kycServiceImpl, "env", env);
		ReflectionTestUtils.setField(authFacadeImpl, "notificationService", notificationService);
		ReflectionTestUtils.setField(authFacadeImpl, "idInfoHelper", idInfoHelper);

		ReflectionTestUtils.setField(notificationService, "env", env);
		ReflectionTestUtils.setField(notificationService, "idTemplateManager", idTemplateManager);
		ReflectionTestUtils.setField(notificationService, "notificationManager", notificationManager);
		ReflectionTestUtils.setField(idInfoHelper, "environment", env);
		ReflectionTestUtils.setField(idInfoHelper, "idMappingConfig", idMappingConfig);
	}

	/**
	 * This class tests the authenticateApplicant method where it checks the IdType
	 * and DemoAuthType.
	 *
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 * @throws IdAuthenticationDaoException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */

	@Test
	public void authenticateApplicantTest() throws IdAuthenticationBusinessException, IdAuthenticationDaoException,
			NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, IOException {

		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("IDA");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authType = new AuthTypeDTO();
		authType.setBio(true);
		authRequestDTO.setRequestedAuth(authType);

		BioIdentityInfoDTO fingerValue = new BioIdentityInfoDTO();
		fingerValue.setValue("finger");
		fingerValue.setSubType("Thumb");
		fingerValue.setType("finger");
		BioIdentityInfoDTO irisValue = new BioIdentityInfoDTO();
		irisValue.setValue("iris img");
		irisValue.setSubType("left");
		irisValue.setType("iris");
		BioIdentityInfoDTO faceValue = new BioIdentityInfoDTO();
		faceValue.setValue("face img");
		faceValue.setSubType("Thumb");
		faceValue.setType("face");
		List<BioIdentityInfoDTO> fingerIdentityInfoDtoList = new ArrayList<BioIdentityInfoDTO>();
		fingerIdentityInfoDtoList.add(fingerValue);
		fingerIdentityInfoDtoList.add(irisValue);
		fingerIdentityInfoDtoList.add(faceValue);

		IdentityDTO identitydto = new IdentityDTO();
		identitydto.setBiometrics(fingerIdentityInfoDtoList);

		RequestDTO requestDTO = new RequestDTO();
		requestDTO.setIdentity(identitydto);

		authRequestDTO.setRequest(requestDTO);

		BioInfo bioinfo = new BioInfo();
		bioinfo.setBioType(BioAuthType.FGR_IMG.getType());
		bioinfo.setDeviceId("123456789");
		bioinfo.setDeviceProviderID("1234567890");

		List<BioInfo> bioInfoList = new ArrayList<BioInfo>();
		bioInfoList.add(bioinfo);
		authRequestDTO.setBioMetadata(bioInfoList);
		Map<String, Object> idRepo = new HashMap<>();
		String uin = "274390482564";
		idRepo.put("uin", uin);
		idRepo.put("registrationId", "1234567890");
		AuthStatusInfo authStatusInfo = new AuthStatusInfo();
		authStatusInfo.setStatus(true);
		authStatusInfo.setErr(Collections.emptyList());
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		Mockito.when(otpAuthServiceImpl.authenticate(authRequestDTO, uin, Collections.emptyMap()))
				.thenReturn(authStatusInfo);
		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(idRepo);
		Mockito.when(idAuthService.processIdType(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(idRepo);
		Mockito.when(idAuthService.getIdRepoByUIN(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(repoDetails());
		Mockito.when(idInfoService.getIdInfo(Mockito.any())).thenReturn(idInfo);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		authResponseDTO.setStatus("Y");

		authResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());

		Mockito.when(idInfoService.getIdInfo(repoDetails())).thenReturn(idInfo);
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.NAME, idInfo)).thenReturn("mosip");
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.EMAIL, idInfo)).thenReturn("mosip");
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.PHONE, idInfo)).thenReturn("mosip");
		Mockito.when(tokenIdGenerator.generateId(Mockito.anyString(), Mockito.anyString()))
				.thenReturn("247334310780728918141754192454591343");
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		Mockito.when(bioAuthService.authenticate(authRequestDTO, uin, idInfo)).thenReturn(authStatusInfo);
		Mockito.when(idTemplateManager.applyTemplate(Mockito.anyString(), Mockito.any())).thenReturn("test");
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authFacadeImpl.authenticateApplicant(authRequestDTO, true);

	}

	/**
	 * This class tests the processAuthType (OTP) method where otp validation
	 * failed.
	 *
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 */

	@Test
	public void processAuthTypeTestFail() throws IdAuthenticationBusinessException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setOtp(false);
		authRequestDTO.setRequestedAuth(authTypeDTO);
		authRequestDTO.setId("1234567");

		ZoneOffset offset = ZoneOffset.MAX;
		authRequestDTO.setRequestTime(Instant.now().atOffset(ZoneOffset.of("+0530")) // offset
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		authRequestDTO.setId("id");
		authRequestDTO.setVersion("1.1");
		authRequestDTO.setPartnerID("1234567890");
		IdentityInfoDTO idInfoDTO = new IdentityInfoDTO();
		idInfoDTO.setLanguage(env.getProperty("mosip.primary.lang-code"));
		idInfoDTO.setValue("John");
		IdentityInfoDTO idInfoDTO1 = new IdentityInfoDTO();
		idInfoDTO1.setLanguage(env.getProperty("mosip.secondary.lang-code"));
		idInfoDTO1.setValue("Mike");
		List<IdentityInfoDTO> idInfoList = new ArrayList<>();
		idInfoList.add(idInfoDTO);
		idInfoList.add(idInfoDTO1);
		IdentityDTO idDTO = new IdentityDTO();
		idDTO.setName(idInfoList);
		idDTO.setUin("234567890123");
		RequestDTO reqDTO = new RequestDTO();
		reqDTO.setIdentity(idDTO);
		authRequestDTO.setRequest(reqDTO);
		authRequestDTO.setTransactionID("1234567890");
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		List<AuthStatusInfo> authStatusList = ReflectionTestUtils.invokeMethod(authFacadeImpl, "processAuthType",
				authRequestDTO, idInfo, "1233", true, "247334310780728918141754192454591343");

		assertTrue(authStatusList.stream().noneMatch(status -> status.isStatus()));
	}

	/**
	 * This class tests the processAuthType (OTP) method where otp validation gets
	 * successful.
	 *
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 */
	@Test
	public void processAuthTypeTestSuccess() throws IdAuthenticationBusinessException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("1234567");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setOtp(true);
		authTypeDTO.setBio(true);
		authTypeDTO.setDemo(true);
		authRequestDTO.setRequestedAuth(authTypeDTO);
		IdentityInfoDTO idInfoDTO = new IdentityInfoDTO();
		idInfoDTO.setLanguage("EN");
		idInfoDTO.setValue("John");
		IdentityInfoDTO idInfoDTO1 = new IdentityInfoDTO();
		idInfoDTO1.setLanguage("fre");
		idInfoDTO1.setValue("Mike");
		List<IdentityInfoDTO> idInfoList = new ArrayList<>();
		idInfoList.add(idInfoDTO);
		idInfoList.add(idInfoDTO1);
		IdentityDTO idDTO = new IdentityDTO();
		idDTO.setName(idInfoList);
		idDTO.setUin("234567890123");
		BioIdentityInfoDTO fingerValue = new BioIdentityInfoDTO();
		fingerValue.setValue("finger");
		fingerValue.setSubType("Thumb");
		fingerValue.setType("finger");
		BioIdentityInfoDTO fingerValue2 = new BioIdentityInfoDTO();
		fingerValue2.setValue("");
		fingerValue2.setSubType("Thumb");
		fingerValue2.setType("finger");
		BioInfo bioinfo = new BioInfo();
		bioinfo.setBioType(BioAuthType.FACE_IMG.getType());
		List<BioInfo> bioInfoList = new ArrayList<BioInfo>();
		bioInfoList.add(bioinfo);
		authRequestDTO.setBioMetadata(bioInfoList);
		List<BioIdentityInfoDTO> fingerIdentityInfoDtoList = new ArrayList<BioIdentityInfoDTO>();
		fingerIdentityInfoDtoList.add(fingerValue);
		fingerIdentityInfoDtoList.add(fingerValue2);

		idDTO.setBiometrics(fingerIdentityInfoDtoList);
		RequestDTO reqDTO = new RequestDTO();
		reqDTO.setIdentity(idDTO);
		String otp = "456789";
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		additionalFactors.setTotp(otp);
		reqDTO.setAdditionalFactors(additionalFactors);
		authRequestDTO.setRequest(reqDTO);
		authRequestDTO.setId("1234567");
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);

		Mockito.when(otpAuthServiceImpl.authenticate(authRequestDTO, "1242", Collections.emptyMap()))
				.thenReturn(AuthStatusInfoBuilder.newInstance().setStatus(true).build());
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<AuthStatusInfo> authStatusList = ReflectionTestUtils.invokeMethod(authFacadeImpl, "processAuthType",
				authRequestDTO, idInfo, "1242", true, "247334310780728918141754192454591343");
		assertTrue(authStatusList.stream().anyMatch(status -> status.isStatus()));
	}

	@Test
	public void processAuthTypeTestFailure() throws IdAuthenticationBusinessException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("1234567");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setOtp(true);
		authRequestDTO.setRequestedAuth(authTypeDTO);

		RequestDTO reqDTO = new RequestDTO();
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		reqDTO.setAdditionalFactors(additionalFactors);
		authRequestDTO.setRequest(reqDTO);
		authRequestDTO.setId("1234567");
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		Mockito.when(otpAuthServiceImpl.authenticate(authRequestDTO, "1242", Collections.emptyMap()))
				.thenReturn(AuthStatusInfoBuilder.newInstance().setStatus(true).build());
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<AuthStatusInfo> authStatusList = ReflectionTestUtils.invokeMethod(authFacadeImpl, "processAuthType",
				authRequestDTO, idInfo, "1242", false, "247334310780728918141754192454591343");
		assertTrue(authStatusList.stream().anyMatch(status -> status.isStatus()));
	}

	@Test
	public void processKycAuthValid() throws IdAuthenticationBusinessException {
		KycAuthRequestDTO kycAuthRequestDTO = new KycAuthRequestDTO();
		KycMetadataDTO dto = new KycMetadataDTO();
		dto.setConsentRequired(Boolean.TRUE);
		dto.setSecondaryLangCode("fra");
		kycAuthRequestDTO.setKycMetadata(dto);
		kycAuthRequestDTO.setId("id");
		kycAuthRequestDTO.setVersion("1.1");
		kycAuthRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		kycAuthRequestDTO.setId("id");
		kycAuthRequestDTO.setPartnerID("1234567890");
		kycAuthRequestDTO.setTransactionID("1234567890");
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setDemo(false);
		authTypeDTO.setOtp(true);
		IdentityInfoDTO idInfoDTO = new IdentityInfoDTO();
		idInfoDTO.setLanguage("EN");
		idInfoDTO.setValue("John");
		IdentityInfoDTO idInfoDTO1 = new IdentityInfoDTO();
		idInfoDTO1.setLanguage("fre");
		idInfoDTO1.setValue("Mike");
		List<IdentityInfoDTO> idInfoList = new ArrayList<>();
		idInfoList.add(idInfoDTO);
		idInfoList.add(idInfoDTO1);

		IdentityDTO idDTO = new IdentityDTO();
		idDTO.setName(idInfoList);
		idDTO.setUin("5134256294");
		RequestDTO request = new RequestDTO();
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		String otp = "123456";
		additionalFactors.setTotp(otp);
		request.setAdditionalFactors(additionalFactors);
		request.setIdentity(idDTO);
		request.setIdentity(idDTO);
		kycAuthRequestDTO.setRequest(request);
		kycAuthRequestDTO.setRequestedAuth(authTypeDTO);
		kycAuthRequestDTO.setRequest(request);
		KycMetadataDTO dtos = new KycMetadataDTO();
		dtos.setConsentRequired(Boolean.TRUE);
		dtos.setSecondaryLangCode("fra");
		kycAuthRequestDTO.setKycMetadata(dtos);

		KycResponseDTO kycResponseDTO = new KycResponseDTO();
		KycAuthResponseDTO kycAuthResponseDTO = new KycAuthResponseDTO();
		kycAuthResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		kycAuthResponseDTO.setTransactionID("34567");
		kycAuthResponseDTO.setErrors(null);
		kycResponseDTO.setTtl(env.getProperty("ekyc.ttl.hours"));
		Map<String, ? extends Object> identity = null;
		kycAuthResponseDTO.setStatus(STATUS_SUCCESS);

		kycAuthResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		kycResponseDTO.setIdentity(idInfo);
		kycAuthResponseDTO.setResponse(kycResponseDTO);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		authResponseDTO.setStatus(STATUS_SUCCESS);
		authResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		authResponseDTO.setStaticToken("234567890");
		authResponseDTO.setErrors(null);
		authResponseDTO.setTransactionID("123456789");
		authResponseDTO.setVersion("1.0");
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(kycAuthRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(kycService.retrieveKycInfo(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(kycResponseDTO);
		Mockito.when(idInfoHelper.getUinOrVidType(kycAuthRequestDTO)).thenReturn(values);
		assertNotNull(authFacadeImpl.processKycAuth(kycAuthRequestDTO, authResponseDTO));

	}

	@Test
	public void processKycAuthInValid() throws IdAuthenticationBusinessException {
		KycAuthRequestDTO kycAuthRequestDTO = new KycAuthRequestDTO();
		KycMetadataDTO dto = new KycMetadataDTO();
		dto.setConsentRequired(Boolean.TRUE);
		dto.setSecondaryLangCode("fra");
		kycAuthRequestDTO.setKycMetadata(dto);
		kycAuthRequestDTO.setId("id");
		kycAuthRequestDTO.setVersion("1.1");
		kycAuthRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		kycAuthRequestDTO.setId("id");
		kycAuthRequestDTO.setPartnerID("1234567890");
		kycAuthRequestDTO.setTransactionID("1234567890");
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setDemo(false);
		authTypeDTO.setOtp(true);
		IdentityInfoDTO idInfoDTO = new IdentityInfoDTO();
		idInfoDTO.setLanguage("EN");
		idInfoDTO.setValue("John");
		IdentityInfoDTO idInfoDTO1 = new IdentityInfoDTO();
		idInfoDTO1.setLanguage("fre");
		idInfoDTO1.setValue("Mike");
		List<IdentityInfoDTO> idInfoList = new ArrayList<>();
		idInfoList.add(idInfoDTO);
		idInfoList.add(idInfoDTO1);

		IdentityDTO idDTO = new IdentityDTO();
		idDTO.setName(idInfoList);
		idDTO.setUin("5134256294");
		RequestDTO request = new RequestDTO();
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		String otp = "123456";
		additionalFactors.setTotp(otp);
		request.setAdditionalFactors(additionalFactors);
		request.setIdentity(idDTO);
		request.setIdentity(idDTO);
		kycAuthRequestDTO.setRequest(request);
		kycAuthRequestDTO.setRequestedAuth(authTypeDTO);
		kycAuthRequestDTO.setRequest(request);
		KycMetadataDTO dtos = new KycMetadataDTO();
		dtos.setConsentRequired(Boolean.TRUE);
		dtos.setSecondaryLangCode("fra");
		kycAuthRequestDTO.setKycMetadata(dtos);

		KycResponseDTO kycResponseDTO = new KycResponseDTO();
		KycAuthResponseDTO kycAuthResponseDTO = new KycAuthResponseDTO();
		kycAuthResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		kycAuthResponseDTO.setTransactionID("34567");
		kycAuthResponseDTO.setErrors(null);
		kycResponseDTO.setTtl(env.getProperty("ekyc.ttl.hours"));
		Map<String, ? extends Object> identity = null;
		kycAuthResponseDTO.setStatus(STATUS_SUCCESS);

		kycAuthResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		kycResponseDTO.setIdentity(idInfo);
		kycAuthResponseDTO.setResponse(kycResponseDTO);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		authResponseDTO.setStatus(STATUS_SUCCESS);
		authResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		authResponseDTO.setStaticToken("234567890");
		authResponseDTO.setErrors(null);
		authResponseDTO.setTransactionID("123456789");
		authResponseDTO.setVersion("1.0");
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(kycAuthRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(kycService.retrieveKycInfo(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(kycResponseDTO);
		Mockito.when(idInfoHelper.getUinOrVidType(kycAuthRequestDTO)).thenReturn(values);
		Map<String, List<IdentityInfoDTO>> entityValue = new HashMap<>();
		Mockito.when(idInfoService.getIdInfo(Mockito.any())).thenReturn(entityValue);
		assertNotNull(authFacadeImpl.processKycAuth(kycAuthRequestDTO, authResponseDTO));

	}

	@Test
	public void processKycAuthRequestNull() throws IdAuthenticationBusinessException {
	KycAuthRequestDTO kycAuthRequestDTO = new KycAuthRequestDTO();
		KycMetadataDTO dto = new KycMetadataDTO();
		dto.setConsentRequired(Boolean.TRUE);
		dto.setSecondaryLangCode("fra");
		kycAuthRequestDTO.setKycMetadata(dto);
		kycAuthRequestDTO.setId("id");
		kycAuthRequestDTO.setVersion("1.1");
		kycAuthRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		kycAuthRequestDTO.setId("id");
		kycAuthRequestDTO.setPartnerID("1234567890");
		kycAuthRequestDTO.setTransactionID("1234567890");
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setDemo(false);
		authTypeDTO.setOtp(true);
		IdentityInfoDTO idInfoDTO = new IdentityInfoDTO();
		idInfoDTO.setLanguage("EN");
		idInfoDTO.setValue("John");
		IdentityInfoDTO idInfoDTO1 = new IdentityInfoDTO();
		idInfoDTO1.setLanguage("fre");
		idInfoDTO1.setValue("Mike");
		List<IdentityInfoDTO> idInfoList = new ArrayList<>();
		idInfoList.add(null);
		idInfoList.add(null);

		IdentityDTO idDTO = new IdentityDTO();
		idDTO.setName(idInfoList);
		idDTO.setVid("5134256294");
		RequestDTO request = new RequestDTO();
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		String otp = "123456";
		additionalFactors.setTotp(otp);
		request.setAdditionalFactors(additionalFactors);
		request.setIdentity(idDTO);
		request.setIdentity(idDTO);
		kycAuthRequestDTO.setRequest(null);
		kycAuthRequestDTO.setRequestedAuth(authTypeDTO);
		kycAuthRequestDTO.setRequest(request);
		KycMetadataDTO dtos = new KycMetadataDTO();
		dtos.setConsentRequired(Boolean.TRUE);
		dtos.setSecondaryLangCode("fra");
		kycAuthRequestDTO.setKycMetadata(dtos);

		KycResponseDTO kycResponseDTO = new KycResponseDTO();
		KycAuthResponseDTO kycAuthResponseDTO = new KycAuthResponseDTO();
		kycAuthResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		kycAuthResponseDTO.setTransactionID("34567");
		kycAuthResponseDTO.setErrors(null);
		kycResponseDTO.setTtl(env.getProperty("ekyc.ttl.hours"));
		Map<String, ? extends Object> identity = null;
		kycAuthResponseDTO.setStatus(STATUS_SUCCESS);

		kycAuthResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		kycResponseDTO.setIdentity(idInfo);
		kycAuthResponseDTO.setResponse(kycResponseDTO);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		authResponseDTO.setStatus(STATUS_SUCCESS);
		authResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		authResponseDTO.setStaticToken("234567890");
		authResponseDTO.setErrors(null);
		authResponseDTO.setTransactionID("123456789");
		authResponseDTO.setVersion("1.0");
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(kycAuthRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(kycService.retrieveKycInfo(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(kycResponseDTO);
		Mockito.when(idInfoHelper.getUinOrVidType(kycAuthRequestDTO)).thenReturn(values);
		Map<String, List<IdentityInfoDTO>> entityValue = new HashMap<>();
		Mockito.when(idInfoService.getIdInfo(Mockito.any())).thenReturn(entityValue);
		assertNotNull(authFacadeImpl.processKycAuth(kycAuthRequestDTO, authResponseDTO));

	}

	@Test
	public void testGetAuditEvent() {
		ReflectionTestUtils.invokeMethod(authFacadeImpl, "getAuditEvent", true);
	}

	@Test
	public void testGetAuditEventInternal() {
		ReflectionTestUtils.invokeMethod(authFacadeImpl, "getAuditEvent", false);
	}

	@Test
	public void testProcessBioAuthType() throws IdAuthenticationBusinessException, IOException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("IDA");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authType = new AuthTypeDTO();
		authType.setBio(true);
		authRequestDTO.setRequestedAuth(authType);

		BioIdentityInfoDTO fingerValue = new BioIdentityInfoDTO();
		fingerValue.setValue("finger");
		fingerValue.setSubType("Thumb");
		fingerValue.setType("finger");
		BioIdentityInfoDTO irisValue = new BioIdentityInfoDTO();
		irisValue.setValue("iris img");
		irisValue.setSubType("left");
		irisValue.setType("iris");
		BioIdentityInfoDTO faceValue = new BioIdentityInfoDTO();
		faceValue.setValue("face img");
		faceValue.setSubType("Thumb");
		faceValue.setType("face");
		List<BioIdentityInfoDTO> fingerIdentityInfoDtoList = new ArrayList<BioIdentityInfoDTO>();
		fingerIdentityInfoDtoList.add(fingerValue);
		fingerIdentityInfoDtoList.add(irisValue);
		fingerIdentityInfoDtoList.add(faceValue);

		IdentityDTO identitydto = new IdentityDTO();
		identitydto.setBiometrics(fingerIdentityInfoDtoList);

		RequestDTO requestDTO = new RequestDTO();
		requestDTO.setIdentity(identitydto);

		authRequestDTO.setRequest(requestDTO);

		BioInfo bioinfo = new BioInfo();
		bioinfo.setBioType(BioAuthType.FGR_IMG.getType());
		bioinfo.setDeviceId("123456789");
		bioinfo.setDeviceProviderID("1234567890");

		List<BioInfo> bioInfoList = new ArrayList<BioInfo>();
		bioInfoList.add(bioinfo);
		authRequestDTO.setBioMetadata(bioInfoList);
		Map<String, Object> idRepo = new HashMap<>();
		String uin = "274390482564";
		idRepo.put("uin", uin);
		idRepo.put("registrationId", "1234567890");
		AuthStatusInfo authStatusInfo = new AuthStatusInfo();
		authStatusInfo.setStatus(true);
		authStatusInfo.setErr(Collections.emptyList());
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		Mockito.when(otpAuthServiceImpl.authenticate(authRequestDTO, uin, Collections.emptyMap()))
				.thenReturn(authStatusInfo);
		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(idRepo);
		Mockito.when(idAuthService.processIdType(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(idRepo);
		Mockito.when(idAuthService.getIdRepoByUIN(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(repoDetails());
		Mockito.when(idInfoService.getIdInfo(Mockito.any())).thenReturn(idInfo);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		authResponseDTO.setStatus("Y");

		authResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());

		Mockito.when(idInfoService.getIdInfo(repoDetails())).thenReturn(idInfo);
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.NAME, idInfo)).thenReturn("mosip");
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.EMAIL, idInfo)).thenReturn("mosip");
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.PHONE, idInfo)).thenReturn("mosip");
		Mockito.when(tokenIdGenerator.generateId(Mockito.anyString(), Mockito.anyString()))
				.thenReturn("247334310780728918141754192454591343");
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		Mockito.when(bioAuthService.authenticate(authRequestDTO, uin, idInfo)).thenReturn(authStatusInfo);
		Mockito.when(idTemplateManager.applyTemplate(Mockito.anyString(), Mockito.any())).thenReturn("test");
		ReflectionTestUtils.invokeMethod(authFacadeImpl, "saveAndAuditBioAuthTxn", authRequestDTO, true, uin,
				IdType.UIN, true, "247334310780728918141754192454591343");
	}

	@Test
	public void testProcessBioAuthTypeFinImg() throws IdAuthenticationBusinessException, IOException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("IDA");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authType = new AuthTypeDTO();
		authType.setBio(true);
		authRequestDTO.setRequestedAuth(authType);

		BioIdentityInfoDTO fingerValue = new BioIdentityInfoDTO();
		fingerValue.setValue("finger");
		fingerValue.setSubType("Thumb");
		fingerValue.setType("finger");
		BioIdentityInfoDTO irisValue = new BioIdentityInfoDTO();
		irisValue.setValue("iris img");
		irisValue.setSubType("left");
		irisValue.setType("iris");
		BioIdentityInfoDTO faceValue = new BioIdentityInfoDTO();
		faceValue.setValue("face img");
		faceValue.setSubType("Thumb");
		faceValue.setType("face");
		List<BioIdentityInfoDTO> fingerIdentityInfoDtoList = new ArrayList<BioIdentityInfoDTO>();
		fingerIdentityInfoDtoList.add(fingerValue);
		fingerIdentityInfoDtoList.add(irisValue);
		fingerIdentityInfoDtoList.add(faceValue);

		IdentityDTO identitydto = new IdentityDTO();
		identitydto.setBiometrics(fingerIdentityInfoDtoList);

		RequestDTO requestDTO = new RequestDTO();
		requestDTO.setIdentity(identitydto);

		authRequestDTO.setRequest(requestDTO);

		BioInfo bioinfo = new BioInfo();
		bioinfo.setBioType(BioAuthType.FGR_IMG.getType());
		bioinfo.setDeviceId("123456789");
		bioinfo.setDeviceProviderID("1234567890");
		BioInfo bioinfo1 = new BioInfo();
		bioinfo1.setBioType(BioAuthType.IRIS_IMG.getType());
		bioinfo1.setDeviceId("123456789");
		bioinfo1.setDeviceProviderID("1234567890");

		List<BioInfo> bioInfoList = new ArrayList<BioInfo>();
		bioInfoList.add(bioinfo);
		bioInfoList.add(bioinfo1);
		authRequestDTO.setBioMetadata(bioInfoList);
		Map<String, Object> idRepo = new HashMap<>();
		String uin = "274390482564";
		idRepo.put("uin", uin);
		idRepo.put("registrationId", "1234567890");
		AuthStatusInfo authStatusInfo = new AuthStatusInfo();
		authStatusInfo.setStatus(true);
		authStatusInfo.setErr(Collections.emptyList());
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		Mockito.when(otpAuthServiceImpl.authenticate(authRequestDTO, uin, Collections.emptyMap()))
				.thenReturn(authStatusInfo);
		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(idRepo);
		Mockito.when(idAuthService.processIdType(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
				.thenReturn(idRepo);
		Mockito.when(idAuthService.getIdRepoByUIN(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(repoDetails());
		Mockito.when(idInfoService.getIdInfo(Mockito.any())).thenReturn(idInfo);
		AuthResponseDTO authResponseDTO = new AuthResponseDTO();
		authResponseDTO.setStatus("Y");

		authResponseDTO.setResponseTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());

		Mockito.when(idInfoService.getIdInfo(repoDetails())).thenReturn(idInfo);
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.NAME, idInfo)).thenReturn("mosip");
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.EMAIL, idInfo)).thenReturn("mosip");
		Mockito.when(idInfoHelper.getEntityInfoAsString(DemoMatchType.PHONE, idInfo)).thenReturn("mosip");
		Mockito.when(tokenIdGenerator.generateId(Mockito.anyString(), Mockito.anyString()))
				.thenReturn("247334310780728918141754192454591343");
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		Mockito.when(bioAuthService.authenticate(authRequestDTO, uin, idInfo)).thenReturn(authStatusInfo);
		Mockito.when(idTemplateManager.applyTemplate(Mockito.anyString(), Mockito.any())).thenReturn("test");
		ReflectionTestUtils.invokeMethod(authFacadeImpl, "saveAndAuditBioAuthTxn", authRequestDTO, true, uin,
				IdType.UIN, true, "247334310780728918141754192454591343");
	}

	@Test
	public void testProcessPinDetails_pinValidationStatusNull() throws IdAuthenticationBusinessException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("mosip.identity.auth");
		String uin = "794138547620";
		authRequestDTO.setId("IDA");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setPin(true);
		authRequestDTO.setRequestedAuth(authTypeDTO);
		String pin = null;
		RequestDTO request = new RequestDTO();
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		additionalFactors.setStaticPin(pin);
		request.setAdditionalFactors(additionalFactors);
		authRequestDTO.setRequest(request);
		Map<String, Object> idRepo = new HashMap<>();
		idRepo.put("uin", uin);
		idRepo.put("registrationId", "1234567890");
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		Mockito.when(idAuthService.processIdType(IdType.UIN.getType(), uin, false)).thenReturn(idRepo);
		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(idRepo);
		Mockito.when(idAuthService.getIdRepoByUIN(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(repoDetails());

		Mockito.when(idInfoService.getIdInfo(repoDetails())).thenReturn(idInfo);
		List<AuthStatusInfo> authStatusList = new ArrayList<>();
		AuthStatusInfo pinValidationStatus = new AuthStatusInfo();
		pinValidationStatus.setStatus(true);
		pinValidationStatus.setErr(Collections.emptyList());
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		Mockito.when(pinAuthService.authenticate(authRequestDTO, uin, Collections.emptyMap()))
				.thenReturn(pinValidationStatus);
		ReflectionTestUtils.invokeMethod(authFacadeImpl, "processPinAuth", authRequestDTO, uin, true, authStatusList,
				IdType.UIN, "247334310780728918141754192454591343");

	}

	@Test
	public void testProcessPinDetails_pinValidationStatusNotNull() throws IdAuthenticationBusinessException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("mosip.identity.auth");
		String uin = "794138547620";
		authRequestDTO.setId("IDA");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setPin(true);
		authRequestDTO.setRequestedAuth(authTypeDTO);
		String pin = "456789";
		RequestDTO request = new RequestDTO();
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		additionalFactors.setStaticPin(pin);
		request.setAdditionalFactors(additionalFactors);
		authRequestDTO.setRequest(request);
		Map<String, Object> idRepo = new HashMap<>();
		idRepo.put("uin", uin);
		idRepo.put("registrationId", "1234567890");
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		Mockito.when(idAuthService.processIdType(IdType.UIN.getType(), uin, false)).thenReturn(idRepo);
		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(idRepo);
		Mockito.when(idAuthService.getIdRepoByUIN(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(repoDetails());

		Mockito.when(idInfoService.getIdInfo(repoDetails())).thenReturn(idInfo);
		List<AuthStatusInfo> authStatusList = new ArrayList<>();
		AuthStatusInfo pinValidationStatus = new AuthStatusInfo();
		pinValidationStatus.setStatus(true);
		pinValidationStatus.setErr(Collections.emptyList());
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		Mockito.when(pinAuthService.authenticate(authRequestDTO, uin, Collections.emptyMap()))
				.thenReturn(pinValidationStatus);
		ReflectionTestUtils.invokeMethod(authFacadeImpl, "processPinAuth", authRequestDTO, uin, true, authStatusList,
				IdType.UIN, "247334310780728918141754192454591343");

	}

	@Test
	public void testProcessPinDetails_pinValidationStatus_false() throws IdAuthenticationBusinessException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setId("mosip.identity.auth");
		String uin = "794138547620";
		authRequestDTO.setId("IDA");
		authRequestDTO.setTransactionID("1234567890");
		authRequestDTO.setPartnerID("64378643");
		authRequestDTO.setRequestTime(ZonedDateTime.now()
				.format(DateTimeFormatter.ofPattern(env.getProperty("datetime.pattern"))).toString());
		AuthTypeDTO authTypeDTO = new AuthTypeDTO();
		authTypeDTO.setPin(true);
		authRequestDTO.setRequestedAuth(authTypeDTO);
		String pin = "456789";
		RequestDTO request = new RequestDTO();
		AdditionalFactorsDTO additionalFactors = new AdditionalFactorsDTO();
		additionalFactors.setStaticPin(pin);
		request.setAdditionalFactors(additionalFactors);
		authRequestDTO.setRequest(request);
		Map<String, Object> idRepo = new HashMap<>();
		idRepo.put("uin", uin);
		idRepo.put("registrationId", "1234567890");
		try {
			Mockito.when(idInfoHelper.getUTCTime(Mockito.anyString())).thenReturn("2019-02-18T12:28:17.078");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<IdentityInfoDTO> list = new ArrayList<IdentityInfoDTO>();
		list.add(new IdentityInfoDTO("en", "mosip"));
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		idInfo.put("name", list);
		idInfo.put("email", list);
		idInfo.put("phone", list);
		Mockito.when(idAuthService.processIdType(IdType.UIN.getType(), uin, false)).thenReturn(idRepo);
		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(idRepo);
		Mockito.when(idAuthService.getIdRepoByUIN(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(repoDetails());

		Mockito.when(idInfoService.getIdInfo(repoDetails())).thenReturn(idInfo);
		List<AuthStatusInfo> authStatusList = new ArrayList<>();
		AuthStatusInfo pinValidationStatus = new AuthStatusInfo();
		pinValidationStatus.setStatus(true);
		pinValidationStatus.setErr(Collections.emptyList());
		Optional<String> value = Optional.of("12345678");
		Mockito.when(idInfoHelper.getUinOrVid(authRequestDTO)).thenReturn(value);
		IdType values = IdType.UIN;
		Mockito.when(idInfoHelper.getUinOrVidType(authRequestDTO)).thenReturn(values);
		pinValidationStatus.setStatus(false);
		pinValidationStatus.setErr(Collections.emptyList());
		Mockito.when(pinAuthService.authenticate(authRequestDTO, uin, Collections.emptyMap()))
				.thenReturn(pinValidationStatus);
		ReflectionTestUtils.invokeMethod(authFacadeImpl, "processPinAuth", authRequestDTO, uin, true, authStatusList,
				IdType.UIN, "247334310780728918141754192454591343");

	}

	private Map<String, Object> repoDetails() {
		Map<String, Object> map = new HashMap<>();
		map.put("uin", "863537");
		return map;
	}
}
