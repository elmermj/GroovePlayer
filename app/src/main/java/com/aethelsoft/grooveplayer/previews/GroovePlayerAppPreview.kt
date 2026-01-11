import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aethelsoft.grooveplayer.presentation.player.layouts.LargeTabletPlayerLayout
import com.aethelsoft.grooveplayer.utils.theme.ui.GroovePlayerTheme

@Preview(showBackground = true, name = "GroovePlayer App Preview")
@Composable
fun GroovePlayerAppPreview() {
    // Note: This preview requires a PlayerViewModel instance
    // For a full preview, I'll need to provide a mock ViewModel
    // or run the app to see the actual UI
    //
    // For now I will tag this to remind me
    // TODO: Preview
    GroovePlayerTheme {
        // Preview placeholder - actual preview requires ViewModel setup
//        LargeTabletPlayerLayout() { }
    }
}