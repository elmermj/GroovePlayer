package com.aethelsoft.grooveplayer.presentation.home.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aethelsoft.grooveplayer.presentation.search.SearchBarViewModel
import com.aethelsoft.grooveplayer.utils.DeviceType
import com.aethelsoft.grooveplayer.utils.theme.icons.XSearch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarComponent(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onRequestDismiss: (() -> Unit)? = null,
    onTextFieldPosition: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    deviceType: com.aethelsoft.grooveplayer.utils.DeviceType? = null,
    isSearchExpanded: Boolean = false,
    viewModel: SearchBarViewModel
) {
    when (deviceType) {
        DeviceType.PHONE -> {
            PhoneSearchBarContent(
                onSearch = onSearch,
                modifier = modifier,
                onExpandedChange = onExpandedChange,
                onRequestDismiss = onRequestDismiss,
                onTextFieldPosition = onTextFieldPosition,
                isSearchExpanded = isSearchExpanded,
                viewModel = viewModel
            )
        }
        DeviceType.TABLET, DeviceType.LARGE_TABLET -> {
            TabletSearchBarContent(
                onSearch = onSearch,
                modifier = modifier,
                onExpandedChange = onExpandedChange,
                onRequestDismiss = onRequestDismiss,
                onTextFieldPosition = onTextFieldPosition,
                viewModel = viewModel
            )
        }
        null -> {
            // Fallback to tablet layout if device type is unknown
            TabletSearchBarContent(
                onSearch = onSearch,
                modifier = modifier,
                onExpandedChange = onExpandedChange,
                onRequestDismiss = onRequestDismiss,
                onTextFieldPosition = onTextFieldPosition,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun PhoneSearchBarContent(
    onSearch: (String) -> Unit,
    modifier: Modifier,
    onExpandedChange: ((Boolean) -> Unit)?,
    onRequestDismiss: (() -> Unit)?,
    onTextFieldPosition: ((androidx.compose.ui.geometry.Rect) -> Unit)?,
    isSearchExpanded: Boolean,
    viewModel: SearchBarViewModel
) {
    val searchViewModel = remember { viewModel }
    var searchQuery by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val suggestions by searchViewModel.suggestions.collectAsState(initial = emptyList())
    val shouldShowIconOnly = !isSearchExpanded && !isFocused && searchQuery.isBlank()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var textFieldBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            android.util.Log.d("SearchBar", "Search expanded on phone, requesting focus")
            kotlinx.coroutines.delay(400)
            repeat(5) { attempt ->
                try {
                    focusRequester.requestFocus()
                    kotlinx.coroutines.delay(150)
                    if (isFocused) {
                        android.util.Log.d("SearchBar", "Focus acquired successfully")
                        return@LaunchedEffect
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SearchBar", "Error requesting focus: ${e.message}", e)
                }
                if (attempt < 4) kotlinx.coroutines.delay(100)
            }
        }
    }

    LaunchedEffect(isFocused, searchQuery) {
        val shouldShow = isFocused && searchQuery.isBlank()
        isExpanded = shouldShow
        onExpandedChange?.invoke(shouldShow)
    }
    
    LaunchedEffect(isExpanded) {
        onExpandedChange?.invoke(isExpanded)
    }
    
    LaunchedEffect(onRequestDismiss) {
        if (onRequestDismiss != null && isExpanded) {
            focusManager.clearFocus()
        }
    }
    
    BoxWithConstraints(
        modifier = modifier
            .then(if (shouldShowIconOnly) Modifier.width(48.dp) else Modifier.widthIn(max = 360.dp))
            .clickable(enabled = !isFocused) {
                if (shouldShowIconOnly) {
                    onExpandedChange?.invoke(true)
                }
                focusRequester.requestFocus()
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        val maxWidth = maxWidth.coerceAtMost(360.dp)
        val density = LocalDensity.current
        val animatedWidth by animateDpAsState(
            targetValue = when {
                shouldShowIconOnly -> 48.dp
                isSearchExpanded || isFocused || searchQuery.isNotBlank() -> maxWidth
                else -> 180.dp
            },
            label = "searchBarWidth",
            animationSpec = tween(durationMillis = 300)
        )
        
        Box(modifier = Modifier.width(animatedWidth)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent)
                    .alpha(if (shouldShowIconOnly) 0.01f else 1f)
                    .focusRequester(focusRequester)
                    .onGloballyPositioned { coordinates ->
                        textFieldBounds = coordinates.boundsInRoot()
                        onTextFieldPosition?.invoke(coordinates.boundsInRoot())
                    }
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused) {
                            isExpanded = false
                            if (searchQuery.isBlank()) {
                                onExpandedChange?.invoke(false)
                            }
                        }
                    },
                placeholder = { Text("Search", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(XSearch, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            scope.launch {
                                try {
                                    searchViewModel.saveQuery(searchQuery)
                                } catch (e: Exception) {
                                    android.util.Log.e("SearchBarComponent", "Error saving query: ${e.message}", e)
                                }
                            }
                            onSearch(searchQuery)
                        }
                    }
                )
            )
            
            if (shouldShowIconOnly) {
                Box(
                    modifier = Modifier.size(48.dp).align(Alignment.CenterEnd)
                ) {
                    IconButton(
                        onClick = {
                            onExpandedChange?.invoke(true)
                            scope.launch {
                                kotlinx.coroutines.delay(400)
                                try {
                                    focusRequester.requestFocus()
                                } catch (e: Exception) {
                                    android.util.Log.e("SearchBar", "Error requesting focus: ${e.message}", e)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(XSearch, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletSearchBarContent(
    onSearch: (String) -> Unit,
    modifier: Modifier,
    onExpandedChange: ((Boolean) -> Unit)?,
    onRequestDismiss: (() -> Unit)?,
    onTextFieldPosition: ((androidx.compose.ui.geometry.Rect) -> Unit)?,
    viewModel: SearchBarViewModel
) {
    val searchViewModel = remember { viewModel }
    var searchQuery by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val suggestions by searchViewModel.suggestions.collectAsState(initial = emptyList())
    val shouldExpand = searchQuery.isNotBlank() || isFocused
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var textFieldBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    LaunchedEffect(isFocused, searchQuery) {
        val shouldShow = isFocused && searchQuery.isBlank()
        isExpanded = shouldShow
        onExpandedChange?.invoke(shouldShow)
    }
    
    LaunchedEffect(isExpanded) {
        onExpandedChange?.invoke(isExpanded)
    }
    
    LaunchedEffect(onRequestDismiss) {
        if (onRequestDismiss != null && isExpanded) {
            focusManager.clearFocus()
        }
    }
    
    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 360.dp)
            .clickable(enabled = !isFocused) {
                focusRequester.requestFocus()
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        val maxWidth = maxWidth.coerceAtMost(360.dp)
        val minWidth = 180.dp
        val density = LocalDensity.current
        val animatedWidth by animateDpAsState(
            targetValue = if (shouldExpand) maxWidth.coerceAtMost(360.dp) else minWidth,
            label = "searchBarWidth",
            animationSpec = tween(durationMillis = 300)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .width(animatedWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Transparent)
                .focusRequester(focusRequester)
                .onGloballyPositioned { coordinates ->
                    textFieldBounds = coordinates.boundsInRoot()
                    onTextFieldPosition?.invoke(coordinates.boundsInRoot())
                }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!focusState.isFocused) {
                        isExpanded = false
                    }
                },
            placeholder = { Text("Search", color = Color.White.copy(alpha = 0.6f)) },
            leadingIcon = {
                Icon(XSearch, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    if (searchQuery.isNotBlank()) {
                        scope.launch {
                            try {
                                searchViewModel.saveQuery(searchQuery)
                            } catch (e: Exception) {
                                android.util.Log.e("SearchBarComponent", "Error saving query: ${e.message}", e)
                            }
                        }
                        onSearch(searchQuery)
                    }
                }
            )
        )
    }
}

