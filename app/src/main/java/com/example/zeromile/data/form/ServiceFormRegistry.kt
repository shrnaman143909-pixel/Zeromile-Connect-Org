package com.example.zeromile.data.form

import com.example.zeromile.data.model.CivicAiRecommendation
import com.example.zeromile.data.model.CivicPriority
import com.example.zeromile.data.model.FormFieldConfig
import com.example.zeromile.data.model.FormFieldType
import com.example.zeromile.data.model.ServiceFormConfig

object ServiceFormRegistry {

    fun getFormConfig(recommendation: CivicAiRecommendation): ServiceFormConfig {
        val name = recommendation.serviceName.lowercase()
        val category = recommendation.categoryName.lowercase()
        val defaultPri = when (recommendation.priority.uppercase()) {
            "EMERGENCY" -> CivicPriority.EMERGENCY
            "URGENT" -> CivicPriority.URGENT
            "HIGH" -> CivicPriority.HIGH
            "LOW" -> CivicPriority.LOW
            else -> CivicPriority.MEDIUM
        }

        return when {
            name.contains("noise") || name.contains("loudspeaker") || category.contains("pollution") -> {
                noisePollutionFormConfig(recommendation, defaultPri)
            }
            name.contains("pothole") || (category.contains("road") && !name.contains("light")) -> {
                potholeFormConfig(recommendation, defaultPri)
            }
            name.contains("garbage") || name.contains("waste") || category.contains("garbage") -> {
                garbageCollectionFormConfig(recommendation, defaultPri)
            }
            name.contains("water") || name.contains("tanker") || name.contains("leak") || category.contains("water") -> {
                waterIssueFormConfig(recommendation, defaultPri)
            }
            name.contains("streetlight") || name.contains("light") || name.contains("lamp") -> {
                streetlightFormConfig(recommendation, defaultPri)
            }
            name.contains("traffic") || category.contains("traffic") -> {
                trafficComplaintFormConfig(recommendation, defaultPri)
            }
            name.contains("food") || category.contains("food") -> {
                foodSafetyFormConfig(recommendation, defaultPri)
            }
            else -> {
                generalGrievanceFormConfig(recommendation, defaultPri)
            }
        }
    }

    private fun noisePollutionFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s1111111-1111-1111-1111-111111111111",
            serviceName = rec.serviceName.ifBlank { "Noise Pollution Complaint" },
            categoryName = "Noise Pollution",
            departmentName = rec.departmentName.ifBlank { "Nagpur Municipal Corporation & Nagpur Police" },
            defaultPriority = priority,
            description = "Noise abatement under Nagpur municipal residential bylaws and Maharashtra Police Act.",
            requiresPhotoOrEvidence = true,
            estimatedResolutionDays = 1,
            submissionNote = "Immediate alert dispatched to Nagpur City Police PCR control and Ward 32 Sanitary Inspector.",
            fields = listOf(
                FormFieldConfig(
                    id = "location",
                    label = "Location / Neighborhood in Nagpur",
                    placeholder = "e.g. Near Dharampeth Coffee House, Ward 32",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location",
                    helpText = "Exact street or colony where noise is emanating"
                ),
                FormFieldConfig(
                    id = "noise_source",
                    label = "Source of Noise Disturbance",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Loudspeaker / DJ Event",
                        "Commercial Generator",
                        "Night Construction / Drilling",
                        "Industrial Machinery",
                        "Religious / Social Gathering"
                    ),
                    defaultValue = "Loudspeaker / DJ Event",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "time_period",
                    label = "Time of Occurrence",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Late Night (10 PM - 6 AM)",
                        "Evening (6 PM - 10 PM)",
                        "Daytime Working Hours",
                        "Continuous / Ongoing"
                    ),
                    defaultValue = "Late Night (10 PM - 6 AM)",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "is_recurring",
                    label = "This is a recurring daily or weekly disturbance",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "true",
                    isRequired = false,
                    helpText = "Check if this happens frequently in your neighborhood"
                ),
                FormFieldConfig(
                    id = "notify_police",
                    label = "Forward dispatch to Nagpur Police Night Patrol",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "true",
                    isRequired = false,
                    helpText = "Nagpur PCR unit will verify sound decibels"
                ),
                FormFieldConfig(
                    id = "evidence",
                    label = "Audio / Video Evidence of Noise",
                    type = FormFieldType.EVIDENCE_ATTACHMENT,
                    isRequired = false,
                    helpText = "Recommended: A short audio snippet helps police verify decibel violations"
                ),
                FormFieldConfig(
                    id = "additional_notes",
                    label = "Specific Landmark / Suspected Venue",
                    placeholder = "e.g. Lawn/Hall name, building number, or open ground",
                    type = FormFieldType.TEXT_AREA,
                    isRequired = false,
                    prefillSource = "summary"
                )
            )
        )
    }

    private fun potholeFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s2222222-2222-2222-2222-222222222222",
            serviceName = rec.serviceName.ifBlank { "Pothole Complaint" },
            categoryName = "Roads & Infrastructure",
            departmentName = rec.departmentName.ifBlank { "Public Works Department (PWD)" },
            defaultPriority = priority,
            description = "Rapid road pothole repair and asphalt patching for citizen traffic safety.",
            requiresPhotoOrEvidence = true,
            estimatedResolutionDays = 2,
            submissionNote = "Assigned to PWD Central Division asphalt patching team.",
            fields = listOf(
                FormFieldConfig(
                    id = "location",
                    label = "Road Name & Nearest Landmark",
                    placeholder = "e.g. West High Court Road, opposite Batukbhai Jewellers",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location",
                    helpText = "Provide the street and identifiable landmark for rapid spot location"
                ),
                FormFieldConfig(
                    id = "pothole_severity",
                    label = "Pothole Hazard Level",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Deep Crater (Two-wheeler hazard)",
                        "Multiple Potholes Cluster",
                        "Surface Tar Broken / Uneven",
                        "Waterlogged Hidden Pothole"
                    ),
                    defaultValue = "Deep Crater (Two-wheeler hazard)",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "road_classification",
                    label = "Road Classification",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Main Thoroughfare / Ring Road",
                        "Residential Colony Road",
                        "Busy Market Street",
                        "School / Hospital Approach"
                    ),
                    defaultValue = "Main Thoroughfare / Ring Road",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "estimated_depth",
                    label = "Estimated Depth",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Under 3 inches",
                        "3 - 6 inches deep",
                        "Over 6 inches (Severe Risk)"
                    ),
                    defaultValue = "3 - 6 inches deep",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "traffic_impact",
                    label = "Causing traffic slowdowns or near accidents",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "true",
                    isRequired = false
                ),
                FormFieldConfig(
                    id = "evidence",
                    label = "Pothole Photo Evidence",
                    type = FormFieldType.EVIDENCE_ATTACHMENT,
                    isRequired = true,
                    helpText = "Photo required by PWD contractors to dispatch asphalt mix volume"
                ),
                FormFieldConfig(
                    id = "additional_notes",
                    label = "Exact Spot Direction (e.g. Northbound lane)",
                    placeholder = "e.g. Left side lane heading towards Law College square",
                    type = FormFieldType.TEXT,
                    isRequired = false,
                    prefillSource = "summary"
                )
            )
        )
    }

    private fun garbageCollectionFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s3333333-3333-3333-3333-333333333333",
            serviceName = rec.serviceName.ifBlank { "Garbage Collection" },
            categoryName = "Solid Waste Management",
            departmentName = rec.departmentName.ifBlank { "NMC Solid Waste Management" },
            defaultPriority = priority,
            description = "Clearing overflowing community bins, street waste dumps, and skipped pickups.",
            requiresPhotoOrEvidence = true,
            estimatedResolutionDays = 1,
            submissionNote = "Routed to the Ward Sanitary Inspector and designated tipper truck driver.",
            fields = listOf(
                FormFieldConfig(
                    id = "location",
                    label = "Garbage Spot / Street Address",
                    placeholder = "e.g. Corner of 4th Cross, Ram Nagar, Ward 32",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location"
                ),
                FormFieldConfig(
                    id = "waste_type",
                    label = "Type of Waste Accumulation",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Overflowing Community Bin",
                        "Illegal Open Street Dump",
                        "Skipped Door-to-Door Pickup",
                        "Dry Leaves & Garden Waste",
                        "Construction Debris / Rubble"
                    ),
                    defaultValue = "Overflowing Community Bin",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "days_accumulated",
                    label = "Days of Accumulation",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "1 - 2 Days",
                        "3 - 5 Days",
                        "More than a Week"
                    ),
                    defaultValue = "3 - 5 Days",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "stray_animals",
                    label = "Stray dogs or cattle feeding at garbage spot",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "true",
                    isRequired = false
                ),
                FormFieldConfig(
                    id = "foul_odor",
                    label = "Foul odor reaching nearby residential houses",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "true",
                    isRequired = false
                ),
                FormFieldConfig(
                    id = "evidence",
                    label = "Photo of Waste Pile",
                    type = FormFieldType.EVIDENCE_ATTACHMENT,
                    isRequired = false,
                    helpText = "Helps dispatch the appropriate size hydraulic tipper"
                )
            )
        )
    }

    private fun waterIssueFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s4444444-4444-4444-4444-444444444444",
            serviceName = rec.serviceName.ifBlank { "Water Supply / Pipeline Leakage" },
            categoryName = "Water Works",
            departmentName = rec.departmentName.ifBlank { "NMC Water Works Department" },
            defaultPriority = priority,
            description = "Pipeline leak repair, contamination resolution, and emergency water tanker requests.",
            requiresPhotoOrEvidence = false,
            estimatedResolutionDays = 2,
            submissionNote = "Assigned to Nagpur Central Water Maintenance division.",
            fields = listOf(
                FormFieldConfig(
                    id = "location",
                    label = "Area / House Landmark in Nagpur",
                    placeholder = "e.g. Near Shiv Temple, Shivaji Nagar, Dharampeth",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location"
                ),
                FormFieldConfig(
                    id = "water_issue_type",
                    label = "Type of Water Issue",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Main Pipeline Burst / Leak",
                        "No Supply / Low Pressure",
                        "Contaminated / Muddy Water",
                        "Emergency Tanker Request",
                        "Broken Municipal Valve"
                    ),
                    defaultValue = "Main Pipeline Burst / Leak",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "water_loss_volume",
                    label = "Leakage Severity / Water Loss",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "High Volume (Street Flooded)",
                        "Continuous Moderate Stream",
                        "Slow Trickle / Seepage"
                    ),
                    defaultValue = "High Volume (Street Flooded)",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "homes_affected",
                    label = "Estimated Homes Impacted",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Single Household",
                        "5 - 15 Houses on Lane",
                        "Entire Colony / Society"
                    ),
                    defaultValue = "5 - 15 Houses on Lane",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "evidence",
                    label = "Photo / Video of Pipeline Leak",
                    type = FormFieldType.EVIDENCE_ATTACHMENT,
                    isRequired = false
                ),
                FormFieldConfig(
                    id = "additional_notes",
                    label = "Municipal Consumer Number (if known)",
                    placeholder = "e.g. NMC Water CAN # 104829",
                    type = FormFieldType.TEXT,
                    isRequired = false
                )
            )
        )
    }

    private fun streetlightFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s_light_01",
            serviceName = rec.serviceName.ifBlank { "Streetlight Outage & Repair" },
            categoryName = "Electrical Department",
            departmentName = rec.departmentName.ifBlank { "NMC Electrical Department" },
            defaultPriority = priority,
            description = "Restoring public street lighting, replacing dead LED bulbs, and fixing loose wiring.",
            requiresPhotoOrEvidence = false,
            estimatedResolutionDays = 2,
            submissionNote = "Assigned to the Ward 32 Streetlight maintenance lineman.",
            fields = listOf(
                FormFieldConfig(
                    id = "location",
                    label = "Street / Lane Landmark",
                    placeholder = "e.g. Lane behind Shankar Nagar Garden",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location"
                ),
                FormFieldConfig(
                    id = "pole_number",
                    label = "Pole Number (Painted on pole if visible)",
                    placeholder = "e.g. NMC-DP-42 or leave blank",
                    type = FormFieldType.TEXT,
                    isRequired = false,
                    helpText = "Look for yellow painted alphanumeric code on the pole"
                ),
                FormFieldConfig(
                    id = "lights_count",
                    label = "Lamps Out of Order",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Single Lamp Only",
                        "2 - 4 Consecutive Poles",
                        "Entire Street in Darkness"
                    ),
                    defaultValue = "Single Lamp Only",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "dark_nights",
                    label = "Duration Without Light",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "1 - 2 Nights",
                        "3 - 7 Nights",
                        "More than 1 Week"
                    ),
                    defaultValue = "1 - 2 Nights",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "safety_hazard",
                    label = "Creates unsafe dark spot for pedestrians & women",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "true",
                    isRequired = false
                )
            )
        )
    }

    private fun trafficComplaintFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s8888888-8888-8888-8888-888888888888",
            serviceName = rec.serviceName.ifBlank { "Traffic Complaint" },
            categoryName = "Traffic & Transport",
            departmentName = rec.departmentName.ifBlank { "Nagpur Traffic Police Branch" },
            defaultPriority = priority,
            description = "Faulty traffic signals, hazardous intersections, and road congestion alerts.",
            requiresPhotoOrEvidence = false,
            estimatedResolutionDays = 1,
            submissionNote = "Forwarded to Nagpur Traffic Control Room and Signal Maintenance Wing.",
            fields = listOf(
                FormFieldConfig(
                    id = "location",
                    label = "Traffic Junction / Square in Nagpur",
                    placeholder = "e.g. Variety Square, Sitabuldi / Law College Chowk",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location"
                ),
                FormFieldConfig(
                    id = "traffic_issue_type",
                    label = "Traffic Problem Identified",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Signal Timer Malfunction",
                        "Signal Completely Off / Blinking",
                        "Illegal Parking Blocking Turn",
                        "Missing Warning Signboard",
                        "Severe Unmanaged Congestion"
                    ),
                    defaultValue = "Signal Timer Malfunction",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "peak_time",
                    label = "Worst Impact Time Window",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Morning Peak (8:30 AM - 11:30 AM)",
                        "Evening Peak (5:30 PM - 9:00 PM)",
                        "School Dispersal Time (1:00 PM - 3:00 PM)",
                        "Continuous All Day"
                    ),
                    defaultValue = "Evening Peak (5:30 PM - 9:00 PM)",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "pedestrian_danger",
                    label = "Dangerous for pedestrians crossing the junction",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "true",
                    isRequired = false
                )
            )
        )
    }

    private fun foodSafetyFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s6666666-6666-6666-6666-666666666666",
            serviceName = rec.serviceName.ifBlank { "Food Safety Complaint" },
            categoryName = "Food Safety & Public Health",
            departmentName = rec.departmentName.ifBlank { "Food & Drug Administration (FDA) Nagpur & NMC" },
            defaultPriority = priority,
            description = "Inspecting unhygienic eateries, contaminated food, and adulteration.",
            requiresPhotoOrEvidence = true,
            estimatedResolutionDays = 3,
            submissionNote = "Assigned to the FDA Food Safety Officer for spot food sampling.",
            fields = listOf(
                FormFieldConfig(
                    id = "establishment_name",
                    label = "Restaurant / Eatery / Vendor Name",
                    placeholder = "e.g. Tasty Treat Corner or Street Stall near Sitabuldi",
                    type = FormFieldType.TEXT,
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "location",
                    label = "Exact Location / Food Street Address",
                    placeholder = "e.g. Outside Metro Station, Sitabuldi",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location"
                ),
                FormFieldConfig(
                    id = "violation_type",
                    label = "Safety Violation Noticed",
                    type = FormFieldType.CHIPS,
                    options = listOf(
                        "Foreign Object in Food",
                        "Stale / Rotten Food Served",
                        "Unhygienic Prep Area / Cockroaches",
                        "Adulterated Cooking Oil / Milk",
                        "Staff Without Hairnets / Gloves"
                    ),
                    defaultValue = "Foreign Object in Food",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "incident_date",
                    label = "When did this occur?",
                    type = FormFieldType.CHIPS,
                    options = listOf("Today", "Yesterday", "Within Past 3 Days"),
                    defaultValue = "Today",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "health_impact",
                    label = "Customer suffered stomach illness or food poisoning",
                    type = FormFieldType.CHECKBOX,
                    defaultValue = "false",
                    isRequired = false
                ),
                FormFieldConfig(
                    id = "evidence",
                    label = "Photo of Food Item / Bill Receipt",
                    type = FormFieldType.EVIDENCE_ATTACHMENT,
                    isRequired = false
                )
            )
        )
    }

    private fun generalGrievanceFormConfig(rec: CivicAiRecommendation, priority: CivicPriority): ServiceFormConfig {
        return ServiceFormConfig(
            serviceId = rec.matchedServiceId ?: "s_general_01",
            serviceName = rec.serviceName.ifBlank { "General Civic Grievance" },
            categoryName = rec.categoryName.ifBlank { "Municipal Affairs" },
            departmentName = rec.departmentName.ifBlank { "Nagpur Municipal Corporation (Central Triage)" },
            defaultPriority = priority,
            description = "General citizen grievance routed to the NMC central redressal cell.",
            requiresPhotoOrEvidence = false,
            estimatedResolutionDays = 4,
            submissionNote = "Central grievance cell will review within 24 hours and assign to the concerned ward inspector.",
            fields = listOf(
                FormFieldConfig(
                    id = "location",
                    label = "Location / Landmark in Nagpur",
                    placeholder = "e.g. Near Dharampeth Market, Ward 32",
                    type = FormFieldType.LOCATION_LANDMARK,
                    isRequired = true,
                    prefillSource = "location"
                ),
                FormFieldConfig(
                    id = "subject",
                    label = "Issue Summary",
                    placeholder = "Brief title of the problem",
                    type = FormFieldType.TEXT,
                    isRequired = true,
                    prefillSource = "summary"
                ),
                FormFieldConfig(
                    id = "urgency_preference",
                    label = "Perceived Urgency",
                    type = FormFieldType.CHIPS,
                    options = listOf("Standard", "Urgent - Requires Fast Action", "Emergency Hazard"),
                    defaultValue = "Standard",
                    isRequired = true
                ),
                FormFieldConfig(
                    id = "evidence",
                    label = "Supporting Photo / Document",
                    type = FormFieldType.EVIDENCE_ATTACHMENT,
                    isRequired = false
                ),
                FormFieldConfig(
                    id = "detailed_notes",
                    label = "Detailed Information",
                    placeholder = "Explain any relevant background, timings, or previous attempts",
                    type = FormFieldType.TEXT_AREA,
                    isRequired = false
                )
            )
        )
    }
}
