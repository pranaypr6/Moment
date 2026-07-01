import re

with open('android/app/src/main/java/com/moment/app/ui/moments/MomentsScreen.kt', 'r') as f:
    content = f.read()

# Add imports for detectTapGestures and pointerInput
imports = """import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.moment.app.util.HapticFeedbackManager
import androidx.compose.ui.platform.LocalContext
"""
content = content.replace("import androidx.compose.animation.*", imports + "import androidx.compose.animation.*")

# Add the HeartBurstOverlay composable
heart_burst = """
@Composable
fun HeartBurstOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), initialScale = 0.3f) + fadeIn(),
        exit = scaleOut(tween(300), targetScale = 1.5f) + fadeOut(tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = HeartRed,
                modifier = Modifier.size(120.dp).shadow(12.dp, CircleShape)
            )
        }
    }
}
"""
content += heart_burst

# Replace Hero Moment Clickable with PointerInput
hero_box_start = content.find("Box(\n            modifier = Modifier\n                .fillMaxWidth()\n                .height(380.dp)")
hero_box_end = content.find(".clickable(interactionSource = interactionSource, indication = null, onClick = onClick)\n        ) {", hero_box_start)
hero_replacement = """.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onDoubleTap = {
                            HapticFeedbackManager.playHeartbeat(context)
                            showHeartBurst = true
                            onFavoriteClick()
                        }
                    )
                }
        ) {"""

# We need to add context and showHeartBurst to ImmersiveHeroMoment
hero_def = "fun ImmersiveHeroMoment(moment: MomentEntity, isPaused: Boolean, partnerId: String, partnerName: String, onClick: () -> Unit)"
hero_def_new = "fun ImmersiveHeroMoment(moment: MomentEntity, isPaused: Boolean, partnerId: String, partnerName: String, onClick: () -> Unit, onFavoriteClick: () -> Unit)"
content = content.replace(hero_def, hero_def_new)

hero_vars = """    val interactionSource = remember { MutableInteractionSource() }"""
hero_vars_new = """    val context = LocalContext.current
    var showHeartBurst by remember { mutableStateOf(false) }
    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            kotlinx.coroutines.delay(600)
            showHeartBurst = false
        }
    }
    val interactionSource = remember { MutableInteractionSource() }"""
content = content.replace(hero_vars, hero_vars_new)
content = content.replace(".clickable(interactionSource = interactionSource, indication = null, onClick = onClick)\n        ) {", hero_replacement)
content = content.replace("AsyncImage(\n                model = moment.imageUrl,\n                contentDescription = \"Latest Moment\",\n                contentScale = ContentScale.Crop,\n                modifier = Modifier.fillMaxSize()\n            )", "AsyncImage(\n                model = moment.imageUrl,\n                contentDescription = \"Latest Moment\",\n                contentScale = ContentScale.Crop,\n                modifier = Modifier.fillMaxSize()\n            )\n            HeartBurstOverlay(visible = showHeartBurst)")

# Replace Timeline Moment Clickable with PointerInput
timeline_def = "fun ImmersiveTimelineMoment("
timeline_vars = """    val interactionSource = remember { MutableInteractionSource() }"""
timeline_vars_new = """    val context = LocalContext.current
    var showHeartBurst by remember { mutableStateOf(false) }
    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            kotlinx.coroutines.delay(600)
            showHeartBurst = false
        }
    }
    val interactionSource = remember { MutableInteractionSource() }"""
content = content.replace(timeline_vars, timeline_vars_new)

timeline_replacement = """.pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = {
                        HapticFeedbackManager.playHeartbeat(context)
                        showHeartBurst = true
                        onFavoriteClick()
                    }
                )
            }"""
content = content.replace(".clickable(interactionSource = interactionSource, indication = null, onClick = onClick),", timeline_replacement + ",")
content = content.replace("AsyncImage(\n                model = moment.imageUrl,\n                contentDescription = null,\n                contentScale = ContentScale.Crop,\n                modifier = Modifier\n                    .fillMaxSize()\n                    .graphicsLayer {\n                        scaleX = 1.2f\n                        scaleY = 1.2f\n                        translationY = parallaxOffset\n                    }\n            )", "AsyncImage(\n                model = moment.imageUrl,\n                contentDescription = null,\n                contentScale = ContentScale.Crop,\n                modifier = Modifier\n                    .fillMaxSize()\n                    .graphicsLayer {\n                        scaleX = 1.2f\n                        scaleY = 1.2f\n                        translationY = parallaxOffset\n                    }\n            )\n            HeartBurstOverlay(visible = showHeartBurst)")

# Fix the ImmersiveHeroMoment call in MomentsScreen
content = content.replace("onClick = { selectedMoment = state.latestMoment }", "onClick = { selectedMoment = state.latestMoment }, onFavoriteClick = { viewModel.toggleFavorite(state.latestMoment.id) }")

# Replace Empty State
empty_state_old = """                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                                    .padding(32.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = HeartRed.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("You are connected!", style = MaterialTheme.typography.headlineMedium, color = TextDeep)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap 'Leave a Moment' to beam a photo\\nstraight to their wallpaper.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }"""

empty_state_new = """                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                                    .padding(horizontal = 32.dp, vertical = 24.dp)
                                    .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Transparent)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = HeartRed.copy(alpha = 0.5f), modifier = Modifier.size(72.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Start the Story", 
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ), 
                                        color = TextDeep
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Your scrapbook is waiting.\\nTap below to take your first photo and magically update their wallpaper.",
                                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }"""
content = content.replace(empty_state_old, empty_state_new)


with open('android/app/src/main/java/com/moment/app/ui/moments/MomentsScreen.kt', 'w') as f:
    f.write(content)

