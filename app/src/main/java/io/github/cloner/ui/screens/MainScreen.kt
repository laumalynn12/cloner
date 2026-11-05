package io.github.cloner.ui.screens

import android.os.UserHandle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloner.model.ProfileState
import io.github.cloner.ui.theme.FrozenBlue
import io.github.cloner.ui.theme.FrozenContainer
import io.github.cloner.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onCreateProfileClick: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    val apps by viewModel.displayedApps.collectAsState()
    val showGuide by viewModel.showSetupGuide.collectAsState()
    val guideMessage by viewModel.guideMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Cloner", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Clean Sandbox & App Cloner",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.showAdbProvisioningGuide() }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Petunjuk ADB")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Muat Ulang")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Status Banner Profil
            ProfileStatusBanner(
                profileState = profileState,
                onCreateProfile = onCreateProfileClick,
                onToggleQuietMode = { user -> viewModel.toggleQuietMode(user) },
                onShowAdbGuide = { viewModel.showAdbProvisioningGuide() }
            )

            // 2. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("Cari aplikasi...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 3. Tab Bar (Mainland vs Sandbox)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("Utama (Mainland)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Sandbox (Terisolasi)") }
                )
            }

            // 4. Daftar Aplikasi
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 1 && profileState is ProfileState.Quiet) {
                            "Sandbox sedang tertidur (Quiet Mode aktif).\nBangunkan profil untuk melihat dan menjalankan aplikasi."
                        } else if (selectedTab == 1 && profileState is ProfileState.NotCreated) {
                            "Sandbox belum dibuat.\nTekan tombol di atas untuk membuat ruang isolasi."
                        } else {
                            "Tidak ada aplikasi ditemukan."
                        },
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps, key = { "${it.userHandle.hashCode()}_${it.packageName}" }) { app ->
                        AppItemView(
                            app = app,
                            isSandboxTab = selectedTab == 1,
                            onLaunch = { viewModel.launchApp(app) },
                            onToggleFreeze = { viewModel.toggleFreeze(app) },
                            onClone = {
                                if (profileState is ProfileState.Active) {
                                    viewModel.cloneApp(app, (profileState as ProfileState.Active).userHandle)
                                } else if (profileState is ProfileState.Quiet) {
                                    viewModel.cloneApp(app, (profileState as ProfileState.Quiet).userHandle)
                                } else {
                                    viewModel.showAdbProvisioningGuide()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showGuide) {
        SetupGuideDialog(
            message = guideMessage,
            onDismiss = { viewModel.dismissSetupGuide() }
        )
    }
}

@Composable
fun ProfileStatusBanner(
    profileState: ProfileState,
    onCreateProfile: () -> Unit,
    onToggleQuietMode: (UserHandle) -> Unit,
    onShowAdbGuide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (profileState) {
                is ProfileState.Active -> MaterialTheme.colorScheme.primaryContainer
                is ProfileState.Quiet -> FrozenContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (profileState) {
                is ProfileState.NotCreated -> {
                    Text(
                        text = "Sandbox Belum Dibuat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Buat profil kerja untuk mengisolasi Google Play Store dan aplikasi secara langsung dari HP (tanpa PC/ADB).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCreateProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buat Sandbox Langsung (1-Klik)", fontWeight = FontWeight.Bold)
                    }
                }

                is ProfileState.Active -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sandbox Aktif",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Ruang isolasi berjalan native di atas kernel Android.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Button(onClick = { onToggleQuietMode(profileState.userHandle) }) {
                            Text("Tidur (Quiet Mode)", fontSize = 12.sp)
                        }
                    }
                }

                is ProfileState.Quiet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sandbox Tertidur (Quiet Mode)",
                                fontWeight = FontWeight.Bold,
                                color = FrozenBlue,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "0% Background CPU & RAM. Notifikasi & update dijeda.",
                                fontSize = 12.sp,
                                color = FrozenBlue.copy(alpha = 0.8f)
                            )
                        }
                        Button(onClick = { onToggleQuietMode(profileState.userHandle) }) {
                            Text("Bangunkan", fontSize = 12.sp)
                        }
                    }
                }

                is ProfileState.Disabled -> {
                    Text(text = "Sandbox Dinonaktifkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
