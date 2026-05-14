package dev.atmos.shared.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.TextPrimary
import dev.atmos.shared.ui.theme.TextSecondary

@Composable
fun AccountCard(
    onExportData: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AtmosCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 20.dp,
    ) {
        Text(
            text = "Account",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )

        Spacer(Modifier.height(12.dp))

        // Export My Data
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExportData)
                .padding(vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.FileDownload,
                contentDescription = null,
                tint = HorizonBlue,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Export My Data",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HorizonBlue,
            )
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0F2F5))

        // Sign Out
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSignOut)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = "Sign Out",
                fontSize = 15.sp,
                color = TextSecondary,
            )
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0F2F5))

        // Delete Account
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDeleteAccount)
                .padding(vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = AlertRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Delete Account",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AlertRed,
            )
        }
    }
}
