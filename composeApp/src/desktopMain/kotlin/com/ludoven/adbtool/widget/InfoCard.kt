package com.ludoven.adbtool.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = modifier
            // 移除 aspectRatio，让高度更具弹性，或者可以设置一个较小的固定高度
            // 尝试设置一个固定高度，让卡片不会过大，但要确保内容能适应
            .height(100.dp) // 调整卡片的高度，使其更紧凑
            .fillMaxWidth(), // 填充父级给的宽度
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp // 柔和的阴影
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp), // 减小内边距，使内容更紧凑
            horizontalAlignment = Alignment.CenterHorizontally, // 内容居中
            verticalArrangement = Arrangement.Center // 垂直居中
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(30.dp) // 减小图标大小
            )
            Spacer(modifier = Modifier.height(4.dp)) // 图标与数值的间距

            // 数值 - 突出显示，优先解决溢出，必要时缩小字体
            Text(
                text = value,
                // 这里使用 h6 的基础样式，但把 fontSize 设小，更灵活处理溢出
                // 可以根据实际最长的文本测试这个 fontSize
                style = MaterialTheme.typography.h6.copy(
                    fontSize = 16.sp, // 再次减小字体大小，以确保长文本能显示全
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colors.onSurface,
                maxLines = 1, // 确保单行显示
                overflow = TextOverflow.Ellipsis // 如果溢出显示省略号
            )
            // 如果 16sp 仍然不够，可以进一步尝试 14sp 或 15sp

            Spacer(modifier = Modifier.height(2.dp)) // 数值与标题的间距 (可以更小)

            // 标题 - 辅助信息
            Text(
                text = title,
                style = MaterialTheme.typography.caption.copy(
                    fontSize = 11.sp, // 标题字体更小
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}