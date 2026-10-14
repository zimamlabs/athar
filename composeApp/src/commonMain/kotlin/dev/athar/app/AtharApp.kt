package dev.athar.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.athar.core.Onboarding
import dev.athar.core.Welcome
import dev.athar.journal.JournalScreen
import dev.athar.notes.NoteView
import dev.athar.notes.NotesScreen
import dev.athar.clipboard.ClipboardScreen
import dev.athar.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Section(val key: String, val icon: ImageVector) {
    Home("nav.home", Icons.Rounded.Home),
    Notes("nav.notes", Icons.Rounded.Description),
    Journal("nav.journal", Icons.Rounded.AutoStories),
    Clipboard("nav.clipboard", Icons.Rounded.ContentPaste),
}

enum class Screen { Main, Search, Insights, Settings, About }

/** Where the app is: one of the sections (the notes maybe filtered), or a screen opened from the side menu. */
class Nav {
    var section by mutableStateOf(Section.Home)
    var noteView by mutableStateOf<NoteView>(NoteView.All)
    var screen by mutableStateOf(Screen.Main)
    /** A note to open when the Notes section shows ("" starts a new one). */
    var openNote by mutableStateOf<String?>(null)
    /** Bumped to move the cursor into the capture box on Home. */
    var captureTick by mutableStateOf(0)

    fun go(s: Section, view: NoteView = NoteView.All) { section = s; noteView = view; screen = Screen.Main; openNote = null }
    fun openNote(id: String) { go(Section.Notes); openNote = id }
}

private val WIDE = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtharApp(store: AtharStore, start: Nav = remember { Nav() }, showSplash: Boolean = true) {
    var splash by remember { mutableStateOf(showSplash) }
    // Decided once when the app opens: the tour on a fresh install, the notes once after an update, else nothing.
    var welcome by remember { mutableStateOf(Onboarding.decide(store.settings.lastSeenVersion, store.settings.tourDone, store.hasData, Brand.VERSION)) }
    // The desktop clipboard is looked at about once a second while recording is on.
    LaunchedEffect(store.clipboardState.supported) {
        if (!store.clipboardState.supported) return@LaunchedEffect
        while (true) { store.clipboardState.poll(); delay(900) }
    }
    AtharTheme(store.settings) {
        val nav = start
        Box(Modifier.onPreviewKeyEvent { e ->
            // Ctrl+Alt+N: jump to the capture box (inside the window; a system-wide shortcut is on the roadmap).
            if (e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.isAltPressed && e.key == Key.N) { nav.go(Section.Home); nav.captureTick++; true } else false
        }) {
            val drawer = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            // Back goes to Home from any other section, then closes; it leaves a side screen or a filtered view first.
            PlatformBackHandler(nav.screen == Screen.Main && nav.section != Section.Home) { nav.go(Section.Home) }
            PlatformBackHandler(nav.screen != Screen.Main) { nav.screen = Screen.Main }
            PlatformBackHandler(nav.screen == Screen.Main && nav.section == Section.Notes && nav.noteView != NoteView.All) { nav.noteView = NoteView.All }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= WIDE
                val content: @Composable () -> Unit = {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                title = { Text(title(nav), fontWeight = FontWeight.SemiBold) },
                                navigationIcon = { if (!wide) IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, t("menu")) } },
                                actions = { IconButton(onClick = { nav.screen = Screen.Search }) { Icon(Icons.Rounded.Search, t("nav.search")) } },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                            )
                        },
                        bottomBar = {
                            if (!wide && nav.screen == Screen.Main) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                                Section.entries.forEach { s ->
                                    NavigationBarItem(selected = nav.section == s, onClick = { nav.go(s) }, icon = { Icon(s.icon, null) }, label = { Text(t(s.key), maxLines = 1) })
                                }
                            }
                        },
                    ) { padding ->
                        // On a wide screen the content keeps a readable width instead of stretching edge to edge.
                        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                            Box(Modifier.fillMaxHeight().widthIn(max = 880.dp)) {
                                when (nav.screen) {
                                    Screen.Search -> SearchScreen(store, nav)
                                    Screen.Insights -> InsightsScreen(store)
                                    Screen.Settings -> SettingsScreen(store)
                                    Screen.About -> AboutScreen(store)
                                    Screen.Main -> when (nav.section) {
                                        Section.Home -> HomeScreen(store, nav)
                                        Section.Notes -> NotesScreen(store.notesState, nav.noteView, nav.openNote, onOpenConsumed = { nav.openNote = null })
                                        Section.Journal -> JournalScreen(store.journalState, store.today)
                                        Section.Clipboard -> ClipboardScreen(store.clipboardState, store.settings.clipboardOn) { store.saveSettings(store.settings.copy(clipboardOn = it)) }
                                    }
                                }
                            }
                        }
                    }
                }
                if (wide) PermanentNavigationDrawer(drawerContent = { PermanentDrawerSheet(Modifier.width(300.dp)) { MenuContent(store, nav) {} } }) { content() }
                else ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet { MenuContent(store, nav) { scope.launch { drawer.close() } } } }) { content() }
            }
            val seen = { store.saveSettings(store.settings.copy(tourDone = true, lastSeenVersion = Brand.VERSION)) }
            if (!splash && (welcome == Welcome.Tour || store.tourOpen)) TourScreen(store) { seen(); welcome = Welcome.Nothing; store.tourOpen = false }
            if (!splash && welcome == Welcome.WhatsNew) WhatsNewDialog(Brand.VERSION) { seen(); welcome = Welcome.Nothing }
            AnimatedVisibility(splash, exit = fadeOut()) { Splash { splash = false } }
        }
    }
}

@Composable
private fun title(nav: Nav): String = when (nav.screen) {
    Screen.Search -> t("nav.search")
    Screen.Insights -> t("nav.insights")
    Screen.Settings -> t("nav.settings")
    Screen.About -> t("nav.about")
    Screen.Main -> when (val v = nav.noteView) {
        is NoteView.Tag -> if (nav.section == Section.Notes) "#${v.name}" else t(nav.section.key)
        is NoteView.Collection -> if (nav.section == Section.Notes) v.name else t(nav.section.key)
        NoteView.Pinned -> if (nav.section == Section.Notes) t("nav.pinned") else t(nav.section.key)
        NoteView.Favorites -> if (nav.section == Section.Notes) t("nav.favorites") else t(nav.section.key)
        NoteView.Recent -> if (nav.section == Section.Notes) t("nav.recent") else t(nav.section.key)
        NoteView.Archived -> if (nav.section == Section.Notes) t("nav.archived") else t(nav.section.key)
        NoteView.Trash -> if (nav.section == Section.Notes) t("nav.trash") else t(nav.section.key)
        NoteView.All -> t(nav.section.key)
    }
}

/** The side menu: the sections, the note views, collections and tags, insights, search and settings. */
@Composable
private fun MenuContent(store: AtharStore, nav: Nav, close: () -> Unit) {
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
            Text(Brand.NAME, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("by ${Brand.MAKER}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF5A1F))
        }
        Section.entries.forEach { s ->
            Entry(s.icon, t(s.key), null, nav.screen == Screen.Main && nav.section == s && (s != Section.Notes || nav.noteView == NoteView.All)) { nav.go(s); close() }
        }
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 10.dp))
        Label(t("nav.notes"))
        fun sel(v: NoteView) = nav.screen == Screen.Main && nav.section == Section.Notes && nav.noteView == v
        val notes = store.notesState.notes
        Entry(Icons.Rounded.PushPin, t("nav.pinned"), notes.count { it.pinned && !it.archived }, sel(NoteView.Pinned)) { nav.go(Section.Notes, NoteView.Pinned); close() }
        Entry(Icons.Rounded.Star, t("nav.favorites"), notes.count { it.favorite && !it.archived }, sel(NoteView.Favorites)) { nav.go(Section.Notes, NoteView.Favorites); close() }
        Entry(Icons.Rounded.History, t("nav.recent"), null, sel(NoteView.Recent)) { nav.go(Section.Notes, NoteView.Recent); close() }
        Entry(Icons.Rounded.Archive, t("nav.archived"), notes.count { it.archived }, sel(NoteView.Archived)) { nav.go(Section.Notes, NoteView.Archived); close() }
        Entry(Icons.Rounded.Delete, t("nav.trash"), store.notesState.trashed.size, sel(NoteView.Trash)) { nav.go(Section.Notes, NoteView.Trash); close() }
        val collections = store.notesState.collections()
        if (collections.isNotEmpty()) {
            Label(t("nav.collections"))
            collections.forEach { (name, count) -> Entry(Icons.Rounded.Folder, name, count, sel(NoteView.Collection(name))) { nav.go(Section.Notes, NoteView.Collection(name)); close() } }
        }
        val tags = store.notesState.tags()
        if (tags.isNotEmpty()) {
            Label(t("nav.tags"))
            tags.forEach { (tag, count) -> Entry(Icons.Rounded.Tag, tag, count, sel(NoteView.Tag(tag))) { nav.go(Section.Notes, NoteView.Tag(tag)); close() } }
        }
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 10.dp))
        Entry(Icons.Rounded.Insights, t("nav.insights"), null, nav.screen == Screen.Insights) { nav.screen = Screen.Insights; close() }
        Entry(Icons.Rounded.Search, t("nav.search"), null, nav.screen == Screen.Search) { nav.screen = Screen.Search; close() }
        Entry(Icons.Rounded.Settings, t("nav.settings"), null, nav.screen == Screen.Settings) { nav.screen = Screen.Settings; close() }
        Entry(Icons.Rounded.Info, t("nav.aboutApp", Brand.NAME), null, nav.screen == Screen.About) { nav.screen = Screen.About; close() }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp))
}

@Composable
private fun Entry(icon: ImageVector, label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null) },
        label = { Text(label, maxLines = 1) },
        badge = { if (count != null && count > 0) Text(n(count), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
