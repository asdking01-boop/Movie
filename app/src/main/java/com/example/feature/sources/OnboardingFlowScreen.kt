package com.example.feature.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.database.ContentSourceEntity
import com.example.core.model.AppStrings
import com.example.core.model.SourceType
import com.example.datasource.connectors.SourceConnectorRegistry
import com.example.ui.theme.KrasBlack
import com.example.ui.theme.KrasCardBorder
import com.example.ui.theme.KrasCharcoal
import com.example.ui.theme.KrasEmerald
import com.example.ui.theme.KrasGold
import com.example.ui.theme.KrasObsidian
import com.example.ui.theme.KrasRed
import com.example.ui.theme.KrasSilver
import com.example.ui.theme.KrasWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingFlowScreen(
    strings: AppStrings,
    onToggleLanguage: () -> Unit,
    onCompleteWithSources: suspend (includeLegalDemo: Boolean, customSource: ContentSourceEntity?) -> Unit
) {
    val scope = rememberCoroutineScope()
    // 0 = Animated Splash, 1 = Welcome & First Source Setup, 2 = Synchronizing Catalog Progress
    var step by remember { mutableIntStateOf(0) }
    var splashScale by remember { mutableFloatStateOf(0.75f) }
    val animatedScale by animateFloatAsState(
        targetValue = splashScale,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    var includeLegalCatalog by remember { mutableStateOf(true) }
    var includeJsonBroadcastFeed by remember { mutableStateOf(true) }
    var customSourceName by remember { mutableStateOf("") }
    var customSourceUrl by remember { mutableStateOf("") }
    var syncProgress by remember { mutableFloatStateOf(0.15f) }
    var syncStatusText by remember { mutableStateOf("Validating content sources...") }

    LaunchedEffect(Unit) {
        splashScale = 1.0f
        delay(1400L)
        step = 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KrasBlack)
    ) {
        // Cinematic Background Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_hero_cosmos_1793910454623),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to KrasBlack.copy(alpha = 0.78f),
                            0.5f to KrasBlack.copy(alpha = 0.88f),
                            1.0f to KrasBlack
                        )
                    )
                )
        )

        // Step 0: Elegant Animated KRAS MOVIES Splash Screen
        AnimatedVisibility(
            visible = step == 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(animatedScale)
            ) {
                Surface(
                    color = KrasRed,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = KrasWhite,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "KRAS MOVIES",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp, letterSpacing = 4.sp),
                    color = KrasWhite
                )
                Text(
                    text = "UNIFIED CINEMA & SERIES PLATFORM",
                    style = MaterialTheme.typography.labelLarge,
                    color = KrasRed,
                    letterSpacing = 2.sp
                )
            }
        }

        // Step 1: Welcome & First Content Source Configuration
        AnimatedVisibility(
            visible = step == 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = KrasRed,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = KrasWhite)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "KRAS MOVIES",
                            style = MaterialTheme.typography.displaySmall,
                            color = KrasWhite
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleLanguage,
                        border = BorderStroke(1.dp, KrasCardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = KrasGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.languageToggleLabel, color = KrasWhite)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = if (strings.isArabic) {
                        "مرحباً بك في تجربة البث السينمائي الموحدة"
                    } else {
                        "Your Authorized Sources. One Native Cinema."
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = KrasWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (strings.isArabic) {
                        "يقوم تطبيق KRAS MOVIES باستيراد وتنظيم الأفلام والمسلسلات من مصادرك المصرحة (واجهات REST و GraphQL وخلاصات JSON و M3U) في واجهة أصلية فاخرة."
                    } else {
                        "Connect your authorized REST/GraphQL APIs, JSON/XML feeds, or M3U playlists. Kras Movies imports metadata and episodes into a unified, high-frame-rate theater experience."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = KrasSilver,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Option 1: Built-In Legal Open Cinema Catalog
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (includeLegalCatalog) KrasRed.copy(alpha = 0.18f) else KrasCharcoal
                    ),
                    border = BorderStroke(1.dp, if (includeLegalCatalog) KrasRed else KrasCardBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeLegalCatalog = !includeLegalCatalog }
                        .testTag("onboarding_legal_catalog_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (includeLegalCatalog) KrasRed else KrasSilver,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) "كتالوج السينما المفتوحة (أفلام ومسلسلات مرخصة)" else "Open Cinema Studio Catalog (Legal HD Demo)",
                                style = MaterialTheme.typography.titleMedium,
                                color = KrasWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Includes 7 Creative Commons HD movies & 3 multi-season series with instant ExoPlayer streaming & subtitles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KrasSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Verified Open Broadcast JSON Feed Connector
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (includeJsonBroadcastFeed) KrasEmerald.copy(alpha = 0.16f) else KrasCharcoal
                    ),
                    border = BorderStroke(1.dp, if (includeJsonBroadcastFeed) KrasEmerald else KrasCardBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeJsonBroadcastFeed = !includeJsonBroadcastFeed }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (includeJsonBroadcastFeed) KrasEmerald else KrasSilver,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) "خلاصة JSON المستوردة (Open Broadcast Feed)" else "Open Broadcast JSON Feed (Imported Source)",
                                style = MaterialTheme.typography.titleMedium,
                                color = KrasWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Demonstrates real-time JSON feed parsing, deduplication, and 'IMPORTED' source attribution alongside sample content.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KrasSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 3: Custom User Source URL
                Card(
                    colors = CardDefaults.cardColors(containerColor = KrasObsidian),
                    border = BorderStroke(1.dp, KrasCardBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (strings.isArabic) "أو أضف رابط مصدر خاص بك الآن (اختياري):" else "Or Add Your Own Authorized HTTPS Source (Optional):",
                            style = MaterialTheme.typography.titleSmall,
                            color = KrasGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customSourceName,
                            onValueChange = { customSourceName = it },
                            label = { Text("Source Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KrasRed,
                                unfocusedBorderColor = KrasCardBorder,
                                focusedTextColor = KrasWhite,
                                unfocusedTextColor = KrasWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customSourceUrl,
                            onValueChange = { customSourceUrl = it },
                            label = { Text("HTTPS Feed / API / M3U URL") },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KrasRed,
                                unfocusedBorderColor = KrasCardBorder,
                                focusedTextColor = KrasWhite,
                                unfocusedTextColor = KrasWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        step = 2
                        scope.launch {
                            syncProgress = 0.25f
                            syncStatusText = "Validating source protocols & capabilities..."
                            delay(350L)
                            syncProgress = 0.65f
                            syncStatusText = "Importing movie posters, seasons, and episode metadata..."
                            val customEntity = if (customSourceName.isNotBlank() && customSourceUrl.isNotBlank()) {
                                val detected = SourceConnectorRegistry.detectSourceTypeFromUrl(customSourceUrl)
                                ContentSourceEntity(
                                    name = customSourceName.trim(),
                                    url = customSourceUrl.trim(),
                                    sourceType = detected.name,
                                    isSampleSource = false
                                )
                            } else if (includeJsonBroadcastFeed) {
                                ContentSourceEntity(
                                    name = "Open Broadcast JSON Syndicate",
                                    url = "kras://open-json-feed",
                                    sourceType = SourceType.JSON_FEED.name,
                                    isSampleSource = false
                                )
                            } else null

                            onCompleteWithSources(includeLegalCatalog, customEntity)
                            syncProgress = 1.0f
                            syncStatusText = "Unified catalog ready!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KrasRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_start_sync_button")
                ) {
                    Text(
                        text = if (strings.isArabic) "بدء المزامنة وفتح السينما" else "Validate, Synchronize & Enter Cinema",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Step 2: Live Synchronization Progress Screen
        AnimatedVisibility(
            visible = step == 2,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KrasObsidian.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, KrasCardBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Surface(
                        color = KrasRed.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = KrasRed,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Synchronizing Unified Catalog",
                        style = MaterialTheme.typography.titleLarge,
                        color = KrasWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KrasSilver,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    LinearProgressIndicator(
                        progress = { syncProgress },
                        color = KrasRed,
                        trackColor = KrasCharcoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
