package dev.atmos.shared.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.atmos.shared.ui.theme.CardShape
import dev.atmos.shared.ui.theme.CardSurface

@Composable
fun AtmosCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = CardShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
        ),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface,
        ),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
