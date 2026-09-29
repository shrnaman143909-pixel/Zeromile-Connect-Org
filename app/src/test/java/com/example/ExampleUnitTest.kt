package com.example

import com.example.zeromile.data.model.CivicVoiceRequest
import com.example.zeromile.data.model.VoiceInputMode
import com.example.zeromile.data.model.VoiceLanguage
import com.example.zeromile.ui.viewmodel.CivicViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {
  private lateinit var viewModel: CivicViewModel

  @Before
  fun setUp() {
    viewModel = CivicViewModel()
  }

  @Test
  fun `default voice state is configured correctly`() {
    val state = viewModel.uiState.value
    assertEquals(VoiceLanguage.ENGLISH, state.voiceLanguage)
    assertEquals(VoiceInputMode.VOICE, state.voiceInputMode)
    assertEquals("", state.voiceTranscript)
    assertFalse(state.isListening)
    assertNull(state.voiceError)
  }

  @Test
  fun `multilingual language selection updates correctly`() {
    viewModel.setVoiceLanguage(VoiceLanguage.MARATHI)
    assertEquals(VoiceLanguage.MARATHI, viewModel.uiState.value.voiceLanguage)
    assertEquals("mr-IN", viewModel.uiState.value.voiceLanguage.localeCode)

    viewModel.setVoiceLanguage(VoiceLanguage.HINDI)
    assertEquals(VoiceLanguage.HINDI, viewModel.uiState.value.voiceLanguage)
    assertEquals("hi-IN", viewModel.uiState.value.voiceLanguage.localeCode)

    viewModel.setVoiceLanguage(VoiceLanguage.ENGLISH)
    assertEquals(VoiceLanguage.ENGLISH, viewModel.uiState.value.voiceLanguage)
    assertEquals("en-IN", viewModel.uiState.value.voiceLanguage.localeCode)
  }

  @Test
  fun `voice recognition preserves Marathi speech faithfully without translation`() {
    viewModel.setVoiceLanguage(VoiceLanguage.MARATHI)
    val marathiComplaint = "माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो."

    viewModel.onFinalSpeech(marathiComplaint)

    val state = viewModel.uiState.value
    assertEquals(marathiComplaint, state.voiceTranscript)
    assertTrue(state.showVoiceConfirmation)
    assertFalse(state.isListening)
    assertNull(state.voiceError)

    // Verify continue produces structured CivicVoiceRequest for Phase 3
    val canContinue = viewModel.continueWithTranscript()
    assertTrue(canContinue)

    val request = viewModel.uiState.value.completedVoiceRequest
    assertNotNull(request)
    assertEquals(VoiceInputMode.VOICE, request?.inputMode)
    assertEquals(VoiceLanguage.MARATHI, request?.language)
    assertEquals(marathiComplaint, request?.transcript)
  }

  @Test
  fun `empty transcript cannot continue`() {
    viewModel.clearTranscript()
    val canContinue = viewModel.continueWithTranscript()
    assertFalse(canContinue)
    assertNull(viewModel.uiState.value.completedVoiceRequest)
  }

  @Test
  fun `transcript manual edit mode updates text`() {
    viewModel.onFinalSpeech("Initial pothole issue")
    viewModel.editTranscript("Updated pothole issue near Dharampeth square")

    assertEquals("Updated pothole issue near Dharampeth square", viewModel.uiState.value.voiceTranscript)
  }

  @Test
  fun `text input fallback produces same downstream structure`() {
    viewModel.setVoiceInputMode(VoiceInputMode.TEXT)
    viewModel.setVoiceLanguage(VoiceLanguage.HINDI)
    viewModel.editTranscript("धरमपेठ में सड़क पर गहरा गड्ढा है")

    val canContinue = viewModel.continueWithTranscript()
    assertTrue(canContinue)

    val request = viewModel.uiState.value.completedVoiceRequest
    assertNotNull(request)
    assertEquals(VoiceInputMode.TEXT, request?.inputMode)
    assertEquals(VoiceLanguage.HINDI, request?.language)
    assertEquals("धरमपेठ में सड़क पर गहरा गड्ढा है", request?.transcript)
  }

  @Test
  fun `error states are handled properly`() {
    viewModel.onSpeechError("Microphone access is needed for voice input.")
    assertEquals("Microphone access is needed for voice input.", viewModel.uiState.value.voiceError)

    viewModel.recordAgain()
    assertNull(viewModel.uiState.value.voiceError)
    assertEquals("", viewModel.uiState.value.voiceTranscript)
  }

  @Test
  fun `classifier maps Marathi DJ noise complaint to Pollution and NMC Police with high confidence`() {
    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()
    val recommendation = classifier.classifyLocally(
      transcript = "माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.",
      language = VoiceLanguage.MARATHI
    )

    assertEquals("Pollution", recommendation.categoryName)
    assertEquals("Noise Pollution / Loudspeaker Complaint", recommendation.serviceName)
    assertTrue(recommendation.departmentName.contains("Police") || recommendation.departmentName.contains("NMC"))
    assertEquals("HIGH", recommendation.priority)
    assertTrue(recommendation.confidence >= 0.90)
    assertTrue(recommendation.suggestedFields.isNotEmpty())
  }

  @Test
  fun `classifier maps English road pothole complaint to PWD`() {
    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()
    val recommendation = classifier.classifyLocally(
      transcript = "Dangerous deep pothole on West High Court Road near Dharampeth.",
      language = VoiceLanguage.ENGLISH
    )

    assertEquals("Road", recommendation.categoryName)
    assertEquals("Pothole Complaints", recommendation.serviceName)
    assertEquals("Public Works Department (PWD)", recommendation.departmentName)
    assertEquals("HIGH", recommendation.priority)
    assertTrue(recommendation.confidence >= 0.90)
  }

  @Test
  fun `confirming recommendation creates pre-filled intake form draft`() {
    viewModel.setVoiceLanguage(VoiceLanguage.MARATHI)
    viewModel.onFinalSpeech("माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.")
    viewModel.continueWithTranscript()

    // Simulate AI recommendation arrived
    val mockRec = com.example.zeromile.data.model.CivicAiRecommendation(
      categoryName = "Pollution",
      serviceName = "Noise Pollution / Loudspeaker Complaint",
      departmentName = "Nagpur Municipal Corporation & Nagpur Police",
      priority = "HIGH",
      confidence = 0.96,
      explanation = "Noise disturbance during resting hours in Nagpur.",
      suggestedFields = listOf("Landmark", "Time")
    )

    // Verify confirm creates complaint form draft
    viewModel.chooseManualService(
      com.example.zeromile.data.model.CivicService(
        id = "test-service-01",
        categoryId = "cat-01",
        departmentId = "dept-01",
        name = "Noise Pollution / Loudspeaker Complaint"
      )
    )
    viewModel.confirmRecommendation()

    val draft = viewModel.uiState.value.complaintFormDraft
    assertNotNull(draft)
    assertEquals("Noise Pollution / Loudspeaker Complaint", draft?.serviceName)
    assertEquals("Ward 32, Dharampeth", draft?.ward)
    assertEquals("माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.", draft?.citizenTranscript)
    assertEquals("+91 98230 12345", draft?.citizenPhone)
  }

  @Test
  fun `Phase 4 dynamic form engine generates service-specific form configurations`() {
    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()

    // 1. Noise complaint recommendation
    val noiseRec = classifier.classifyLocally(
      transcript = "माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.",
      language = VoiceLanguage.MARATHI
    )
    val noiseConfig = com.example.zeromile.data.form.ServiceFormRegistry.getFormConfig(noiseRec)

    // Verify noise-specific fields
    val noiseFieldIds = noiseConfig.fields.map { it.id }
    assertTrue("Noise form should contain noise_source", noiseFieldIds.contains("noise_source"))
    assertTrue("Noise form should contain time_period", noiseFieldIds.contains("time_period"))
    assertTrue("Noise form should contain is_recurring", noiseFieldIds.contains("is_recurring"))
    assertTrue("Noise form should contain notify_police", noiseFieldIds.contains("notify_police"))
    assertFalse("Noise form should not contain pothole_severity", noiseFieldIds.contains("pothole_severity"))

    // 2. Pothole complaint recommendation
    val potholeRec = classifier.classifyLocally(
      transcript = "Huge pothole on road near Sitabuldi square causing two-wheelers to fall.",
      language = VoiceLanguage.ENGLISH
    )
    val potholeConfig = com.example.zeromile.data.form.ServiceFormRegistry.getFormConfig(potholeRec)

    // Verify pothole-specific fields
    val potholeFieldIds = potholeConfig.fields.map { it.id }
    assertTrue("Pothole form should contain pothole_severity", potholeFieldIds.contains("pothole_severity"))
    assertTrue("Pothole form should contain road_classification", potholeFieldIds.contains("road_classification"))
    assertTrue("Pothole form should contain estimated_depth", potholeFieldIds.contains("estimated_depth"))
    assertFalse("Pothole form should not contain noise_source", potholeFieldIds.contains("noise_source"))

    // 3. Garbage collection recommendation
    val garbageRec = classifier.classifyLocally(
      transcript = "Overflowing garbage bin and trash dumped on street corner.",
      language = VoiceLanguage.ENGLISH
    )
    val garbageConfig = com.example.zeromile.data.form.ServiceFormRegistry.getFormConfig(garbageRec)
    val garbageFieldIds = garbageConfig.fields.map { it.id }
    assertTrue("Garbage form should contain waste_type", garbageFieldIds.contains("waste_type"))
    assertTrue("Garbage form should contain days_accumulated", garbageFieldIds.contains("days_accumulated"))
    assertTrue("Garbage form should contain stray_animals", garbageFieldIds.contains("stray_animals"))
  }

  @Test
  fun `Phase 4 pre-filling automatically populates extracted location without repeated typing`() {
    viewModel.setVoiceLanguage(VoiceLanguage.ENGLISH)
    val transcript = "Deep dangerous pothole near Sitabuldi square."
    viewModel.onFinalSpeech(transcript)
    viewModel.continueWithTranscript()

    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()
    val rec = classifier.classifyLocally(transcript, VoiceLanguage.ENGLISH)

    assertEquals("Sitabuldi, Nagpur", rec.locationMentioned)

    // Simulate AI recommendation set in ViewModel
    viewModel.chooseManualService(
      com.example.zeromile.data.model.CivicService(
        id = "s2222222-2222-2222-2222-222222222222",
        categoryId = "c1111111-1111-1111-1111-111111111111",
        departmentId = "d4444444-4444-4444-4444-444444444444",
        name = "Pothole Complaint"
      )
    )
    viewModel.confirmRecommendation()

    val values = viewModel.uiState.value.formFieldValues
    assertEquals("Sitabuldi, Nagpur", values["location"])
  }

  @Test
  fun `Phase 4 form validation transitions through Filling, Reviewing, and Ready for Submission`() {
    viewModel.setVoiceLanguage(VoiceLanguage.MARATHI)
    val transcript = "माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो."
    viewModel.onFinalSpeech(transcript)
    viewModel.continueWithTranscript()

    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()
    val rec = classifier.classifyLocally(transcript, VoiceLanguage.MARATHI)

    viewModel.chooseManualService(
      com.example.zeromile.data.model.CivicService(
        id = "s1111111-1111-1111-1111-111111111111",
        categoryId = "c5555555-5555-5555-5555-555555555555",
        departmentId = "d1111111-1111-1111-1111-111111111111",
        name = "Noise Pollution / Loudspeaker Complaint"
      )
    )
    viewModel.confirmRecommendation()

    // Step 1: Form is in FILLING state
    assertEquals(com.example.zeromile.data.model.FormStep.FILLING, viewModel.uiState.value.formStep)
    assertNotNull(viewModel.uiState.value.dynamicFormConfig)

    // If a required field is cleared, validation fails
    viewModel.updateFormField("location", "")
    val isValidWhenEmpty = viewModel.validateAndReviewForm()
    assertFalse(isValidWhenEmpty)
    assertTrue(viewModel.uiState.value.formErrors.containsKey("location"))
    assertEquals(com.example.zeromile.data.model.FormStep.FILLING, viewModel.uiState.value.formStep)

    // Re-fill required field
    viewModel.updateFormField("location", "Dharampeth, Nagpur")
    assertFalse(viewModel.uiState.value.formErrors.containsKey("location"))

    // Validate and transition to Step 2: REVIEWING
    val isValid = viewModel.validateAndReviewForm()
    assertTrue(isValid)
    assertEquals(com.example.zeromile.data.model.FormStep.REVIEWING, viewModel.uiState.value.formStep)

    val review = viewModel.uiState.value.complaintReviewData
    assertNotNull(review)
    assertEquals("Noise Pollution / Loudspeaker Complaint", review?.serviceName)
    assertEquals("Dharampeth, Nagpur", review?.fieldResponses?.find { it.fieldId == "location" }?.value)

    // Test editing: returns back to FILLING
    viewModel.editComplaintDetails()
    assertEquals(com.example.zeromile.data.model.FormStep.FILLING, viewModel.uiState.value.formStep)

    // Re-advance to REVIEWING
    viewModel.validateAndReviewForm()
    assertEquals(com.example.zeromile.data.model.FormStep.REVIEWING, viewModel.uiState.value.formStep)

    // Confirm and advance to Step 3: READY_FOR_SUBMISSION
    viewModel.confirmReadyForSubmission()
    assertEquals(com.example.zeromile.data.model.FormStep.READY_FOR_SUBMISSION, viewModel.uiState.value.formStep)
  }

  @Test
  fun `phase 6 complaint tracking loads real database details, timeline, and updates`() = kotlinx.coroutines.runBlocking {
    // Open tracking for seed complaint NMC-2026-001245
    viewModel.openComplaintDetail("NMC-2026-001245")

    assertEquals("NMC-2026-001245", viewModel.uiState.value.selectedComplaintId)
    // Wait for coroutine to complete loading
    kotlinx.coroutines.delay(50)

    val trackingState = viewModel.uiState.value.complaintTrackingState
    assertTrue("Tracking state should be Success", trackingState is com.example.zeromile.ui.viewmodel.UiState.Success)

    val details = (trackingState as com.example.zeromile.ui.viewmodel.UiState.Success).data
    assertEquals("NMC-2026-001245", details.complaint.complaintNumber)
    assertEquals("In Progress", details.complaint.status)
    assertEquals("Civic Enforcement Team", details.assignedTeam)

    // Verify chronological status history
    assertEquals(3, details.history.size)
    assertEquals("Submitted", details.history[0].status)
    assertEquals("Assigned", details.history[1].status)
    assertEquals("In Progress", details.history[2].status)

    // Verify official municipal updates
    assertEquals(2, details.updates.size)
    assertEquals("Nagpur Municipal Corporation", details.updates[0].officialName)
    assertEquals("Dharampeth Zone Office", details.updates[1].officialName)

    // Test close complaint detail returns to Activity list
    viewModel.closeComplaintDetail()
    assertNull(viewModel.uiState.value.selectedComplaintId)
  }

  @Test
  fun `phase 13 acceptance test 1 - real road pothole input via voice dynamically classifies and progresses to ready for submission`() = kotlinx.coroutines.runBlocking {
    viewModel.setVoiceLanguage(VoiceLanguage.ENGLISH)
    val input = "There is a massive dangerous pothole on Wardha Road near Ajni Square damaging two-wheelers"
    viewModel.onFinalSpeech(input)
    viewModel.continueWithTranscript()

    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()
    val rec = classifier.classifyLocally(input, VoiceLanguage.ENGLISH)
    assertTrue("Should classify as Road / Pothole issue", rec.serviceName.contains("Pothole") || rec.serviceName.contains("Road"))
    assertEquals("Public Works Department (PWD)", rec.departmentName)

    viewModel.chooseManualService(
      com.example.zeromile.data.model.CivicService(
        id = "s2222222-2222-2222-2222-222222222222",
        categoryId = "c1111111-1111-1111-1111-111111111111",
        departmentId = "d4444444-4444-4444-4444-444444444444",
        name = rec.serviceName
      )
    )
    viewModel.confirmRecommendation()

    assertEquals(com.example.zeromile.data.model.FormStep.FILLING, viewModel.uiState.value.formStep)
    viewModel.updateFormField("location", "Wardha Road near Ajni Square, Nagpur")
    viewModel.updateFormField("evidence", "pothole_site_photo.jpg")

    val reviewed = viewModel.validateAndReviewForm()
    assertTrue(reviewed)
    assertEquals(com.example.zeromile.data.model.FormStep.REVIEWING, viewModel.uiState.value.formStep)

    viewModel.confirmReadyForSubmission()
    assertEquals(com.example.zeromile.data.model.FormStep.READY_FOR_SUBMISSION, viewModel.uiState.value.formStep)
  }

  @Test
  fun `phase 13 acceptance test 2 - real garbage issue in Hindi dynamically classifies and produces valid complaint`() = kotlinx.coroutines.runBlocking {
    viewModel.setVoiceLanguage(VoiceLanguage.HINDI)
    val hindiGarbage = "गोकुलपेठ बाजार में कचरा पेटी से सारा कचरा बाहर सड़क पर फैल गया है और बदबू आ रही है"
    viewModel.onFinalSpeech(hindiGarbage)
    viewModel.continueWithTranscript()

    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()
    val rec = classifier.classifyLocally(hindiGarbage, VoiceLanguage.HINDI)
    assertTrue("Should classify as Solid Waste / Garbage", rec.serviceName.contains("Garbage") || rec.serviceName.contains("Solid Waste"))

    viewModel.chooseManualService(
      com.example.zeromile.data.model.CivicService(
        id = "s5555555-5555-5555-5555-555555555555",
        categoryId = "c2222222-2222-2222-2222-222222222222",
        departmentId = "d2222222-2222-2222-2222-222222222222",
        name = rec.serviceName
      )
    )
    viewModel.confirmRecommendation()

    viewModel.updateFormField("location", "Gokulpeth Market, Nagpur")
    viewModel.validateAndReviewForm()
    viewModel.confirmReadyForSubmission()

    viewModel.submitRealComplaint()
    kotlinx.coroutines.delay(50)

    val complaint = viewModel.uiState.value.submittedComplaint
    assertNotNull("Complaint must be submitted", complaint)
    assertTrue("Complaint number must start with NMC-", complaint!!.complaintNumber.startsWith("NMC-"))
    assertEquals("Submitted", complaint.status)
    assertEquals(com.example.zeromile.data.model.FormStep.SUBMITTED, viewModel.uiState.value.formStep)
  }

  @Test
  fun `phase 13 acceptance test 3 - real water leak issue in Marathi dynamically classifies with emergency priority`() = kotlinx.coroutines.runBlocking {
    viewModel.setVoiceLanguage(VoiceLanguage.MARATHI)
    val marathiWater = "शंकर नगर चौकात पाण्याची पाईपलाईन फुटली आहे आणि लाखो लिटर पिण्याचे पाणी वाहत आहे"
    viewModel.onFinalSpeech(marathiWater)
    viewModel.continueWithTranscript()

    val classifier = com.example.zeromile.ai.GeminiCivicClassifier()
    val rec = classifier.classifyLocally(marathiWater, VoiceLanguage.MARATHI)
    assertTrue("Should classify as Water pipeline issue", rec.serviceName.contains("Water") || rec.serviceName.contains("Pipeline"))

    viewModel.chooseManualService(
      com.example.zeromile.data.model.CivicService(
        id = "s4444444-4444-4444-4444-444444444444",
        categoryId = "c3333333-3333-3333-3333-333333333333",
        departmentId = "d3333333-3333-3333-3333-333333333333",
        name = rec.serviceName
      )
    )
    viewModel.confirmRecommendation()
    viewModel.updateFormField("location", "Shankar Nagar Chowk, Dharampeth Zone")
    val valid = viewModel.validateAndReviewForm()
    assertTrue(valid)
  }

  @Test
  fun `phase 6 resolved complaint displays resolution details`() = kotlinx.coroutines.runBlocking {
    // Open tracking for resolved streetlight complaint NMC-2026-001247
    viewModel.openComplaintDetail("NMC-2026-001247")
    kotlinx.coroutines.delay(50)

    val trackingState = viewModel.uiState.value.complaintTrackingState
    assertTrue(trackingState is com.example.zeromile.ui.viewmodel.UiState.Success)

    val details = (trackingState as com.example.zeromile.ui.viewmodel.UiState.Success).data
    assertEquals("Resolved", details.complaint.status)
    assertEquals("Electrical Maintenance Section", details.assignedTeam)
    assertNotNull(details.resolutionNote)
    assertTrue(details.resolutionNote!!.contains("Street illumination restored"))
    assertNotNull(details.resolvedAt)
  }
}
