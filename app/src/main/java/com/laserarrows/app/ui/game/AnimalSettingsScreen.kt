package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.components.CenteredScroll
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.data.AnimalSetup
import com.laserarrows.app.data.AnimalStore
import com.laserarrows.app.data.CustomAnimal
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.ui.components.AdaptiveScreen
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.HomeIconButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.AnimalCatalog
import com.laserarrows.app.ui.theme.AnimalLook
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.painter
import com.laserarrows.app.ui.theme.toComposeColor

/**
 * どうぶつの設定(Lv.20到達で解放)。上段で色を選び、下の一覧から動物をタップするとその色の動物が変わる。
 * 最初の6匹以外は、Lv.20で1匹、以後5レベルごとに1匹ずつ選べるようになる(まだの動物は鍵つきで見せておく)。
 * 写真から動物を作れるのは、用意した動物がすべてそろった次の枠(最後の解放)。
 * 写真から自分だけの動物を作って加えることもできる。6色は必ずすべて違う動物になる(選んだ動物を他の色が使っていたら入れ替わる)。
 */
@Composable
fun AnimalSettingsScreen(
    setup: AnimalSetup,
    levelReached: Int,
    onBack: () -> Unit,
    onAssign: (LaserColor, String) -> Unit,
    onReset: () -> Unit,
    onAddCustom: (uri: Uri, name: String, color: LaserColor, onDone: (Boolean) -> Unit) -> Unit,
    onRenameCustom: (id: String, name: String) -> Unit,
    onDeleteCustom: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(LaserColor.RED) }
    var pendingPhoto by remember { mutableStateOf<Uri?>(null) }
    var editing by remember { mutableStateOf<CustomAnimal?>(null) }
    var confirmingReset by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val keys = setup.resolvedKeys
    val colorOfKey = keys.entries.associate { (color, key) -> key to color }
    val canAddMore = setup.customs.size < AnimalStore.MAX_CUSTOMS
    val photoUnlocked = levelReached >= AnimalCatalog.PHOTO_UNLOCK_LEVEL

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingPhoto = uri
    }

    Box(modifier = modifier.fillMaxSize()) {
        AdaptiveScreen(
            portraitPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            spacing = 10.dp,
            sideWidth = 320.dp,
            header = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeIconButton(onClick = onBack)
                Image(painterResource(R.drawable.icon_paw), contentDescription = null, modifier = Modifier.size(32.dp))
                GameText("どうぶつの設定", fontSize = 24.sp, color = TextPrimary, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
            }

            // いまのなかま(6色)。タップした色が、下の一覧で選ぶ動物の行き先になる。
            CreamCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("いまのなかま", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        LaserColor.entries.forEach { color ->
                            TeamSlot(
                                look = setup.looks.of(color),
                                color = color,
                                selected = color == selected,
                                onClick = {
                                    selected = color
                                    message = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        message?.first ?: "${setup.looks.of(selected).name}の色を選んでいます。一覧から動物をタップしてね。",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (message?.second == true) DangerColor else TextMuted,
                    )
                }
            }

            },
            footer = {
                GhostButton("最初の6匹に戻す", onClick = { confirmingReset = true }, modifier = Modifier.fillMaxWidth())
            },
        ) { gridModifier ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 68.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = gridModifier,
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("じぶんの動物 (${setup.customs.size}/${AnimalStore.MAX_CUSTOMS})") }
                item {
                    AnimalTile(
                        painter = painterResource(R.drawable.icon_camera),
                        name = when {
                            !photoUnlocked -> "Lv.${AnimalCatalog.PHOTO_UNLOCK_LEVEL}"
                            adding -> "つくり中…"
                            else -> "写真から"
                        },
                        usedBy = null,
                        selectedColor = null,
                        enabled = !photoUnlocked || (canAddMore && !adding),
                        locked = !photoUnlocked,
                        onClick = {
                            if (photoUnlocked) {
                                message = null
                                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                message = "写真から動物を作れるのは、Lv.${AnimalCatalog.PHOTO_UNLOCK_LEVEL}に到達してからだよ。" to false
                            }
                        },
                    )
                }
                items(setup.customs, key = { "c:" + it.id }) { custom ->
                    val key = AnimalLook.customKey(custom.id)
                    AnimalTile(
                        painter = custom.look.painter(),
                        name = custom.name,
                        usedBy = colorOfKey[key],
                        selectedColor = selected,
                        onClick = {
                            message = null
                            onAssign(selected, key)
                        },
                        onEdit = { editing = custom },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle("どうぶつ (${AnimalCatalog.unlockedCount(levelReached)}/${AnimalCatalog.builtins.size}種類)")
                }
                items(AnimalCatalog.builtins, key = { "b:" + it.id }) { animal ->
                    val key = AnimalLook.builtinKey(animal.id)
                    val unlockLevel = AnimalCatalog.unlockLevel(animal)
                    val unlocked = levelReached >= unlockLevel
                    AnimalTile(
                        painter = painterResource(animal.res),
                        name = if (unlocked) animal.name else "Lv.$unlockLevel",
                        usedBy = colorOfKey[key],
                        selectedColor = selected,
                        locked = !unlocked,
                        onClick = {
                            if (unlocked) {
                                message = null
                                onAssign(selected, key)
                            } else {
                                message = "${animal.name}はLv.${unlockLevel}に到達するとなかまになるよ。" to false
                            }
                        },
                    )
                }
            }
        }

        pendingPhoto?.let { uri ->
            NameDialog(
                title = "新しいなかまの名前",
                initial = "なかま${setup.customs.size + 1}",
                confirmLabel = "登録する",
                onConfirm = { name ->
                    pendingPhoto = null
                    adding = true
                    val target = selected
                    onAddCustom(uri, name, target) { ok ->
                        adding = false
                        message = if (ok) null else ("写真を読み込めませんでした。別の写真で試してね。" to true)
                    }
                },
                onCancel = { pendingPhoto = null },
            )
        }

        editing?.let { custom ->
            NameDialog(
                title = "名前を変える",
                initial = custom.name,
                confirmLabel = "変更する",
                onConfirm = { name ->
                    onRenameCustom(custom.id, name)
                    editing = null
                },
                onCancel = { editing = null },
                onDelete = {
                    onDeleteCustom(custom.id)
                    editing = null
                },
            )
        }

        if (confirmingReset) {
            DialogScrim {
                Text("最初の6匹に戻しますか？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Text(
                    "キツネ・ペンギン・カエル・ライオン・ウサギ・クマに戻ります。写真から作った動物は消えません。",
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("キャンセル", onClick = { confirmingReset = false }, modifier = Modifier.weight(1f))
                    NeonButton(
                        "戻す",
                        onClick = {
                            onReset()
                            confirmingReset = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.padding(top = 4.dp))
}

/** いまのなかまの1色分。色の丸の上に動物、下に名前。選択中は太い縁で囲む。 */
@Composable
private fun TeamSlot(look: AnimalLook, color: LaserColor, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = color.toComposeColor()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(tint.copy(alpha = if (selected) 0.95f else 0.5f), CircleShape)
                .border(if (selected) 3.dp else 1.5.dp, if (selected) TextPrimary else Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(look.painter(), contentDescription = look.name, modifier = Modifier.fillMaxSize().padding(6.dp))
        }
        Text(
            look.name,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 一覧の動物1匹分。いずれかの色が使っている動物には、その色の丸印をつける。
 * いま選んでいる色が使っている動物は、その色の太い縁で囲む。
 */
@Composable
private fun AnimalTile(
    painter: Painter,
    name: String,
    usedBy: LaserColor?,
    selectedColor: LaserColor?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    locked: Boolean = false,
    onEdit: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val isSelectedOne = usedBy != null && usedBy == selectedColor
    Box(modifier = Modifier.alpha(if (enabled) 1f else 0.45f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassFill, shape)
                .border(
                    width = if (isSelectedOne) 3.dp else 1.5.dp,
                    color = if (isSelectedOne) usedBy!!.toComposeColor() else GlassBorder,
                    shape = shape,
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                // まだ選べない動物は、影絵のように暗くして鍵をつける(何が来るかの楽しみは残しつつ、形で期待させる)。
                Image(
                    painter,
                    contentDescription = name,
                    colorFilter = if (locked) ColorFilter.tint(Color(0xFF8C6C52).copy(alpha = 0.55f)) else null,
                    modifier = Modifier.fillMaxSize(),
                )
                if (locked) {
                    Image(
                        painterResource(R.drawable.icon_lock),
                        contentDescription = "未解放",
                        modifier = Modifier.size(20.dp).align(Alignment.BottomEnd),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (locked) TextMuted else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (usedBy != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(14.dp)
                    .background(usedBy.toComposeColor(), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
        if (onEdit != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, GlassBorder, CircleShape)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.icon_pencil), contentDescription = "名前を変える・消す", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial) }
    var confirmingDelete by remember { mutableStateOf(false) }
    DialogScrim {
        if (confirmingDelete) {
            Text("「$initial」を消しますか？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary, textAlign = TextAlign.Center)
            Text(
                "この動物を使っていた色は、ほかの動物に戻ります。",
                fontSize = 12.5.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("やめる", onClick = { confirmingDelete = false }, modifier = Modifier.weight(1f))
                NeonButton("消す", onClick = { onDelete?.invoke() }, face = DangerColor, edge = Color(0xFFB8293D), modifier = Modifier.weight(1f))
            }
        } else {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace("\n", "").take(AnimalStore.MAX_NAME_LENGTH) },
                singleLine = true,
                supportingText = { Text("${name.length}/${AnimalStore.MAX_NAME_LENGTH}文字", fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("キャンセル", onClick = onCancel, modifier = Modifier.weight(1f))
                NeonButton(confirmLabel, onClick = { onConfirm(name) }, enabled = name.isNotBlank(), modifier = Modifier.weight(1f))
            }
            if (onDelete != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { confirmingDelete = true }.padding(4.dp),
                ) {
                    Image(painterResource(R.drawable.icon_trash), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" この動物を消す", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = DangerColor)
                }
            }
        }
    }
}

/** 画面を薄暗くして、真ん中にクリーム色の札を出す(ホームの確認ダイアログと同じ見た目)。 */
@Composable
private fun DialogScrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x993B2414))
            .clickable(enabled = true, onClick = {}),
    ) {
        CenteredScroll(modifier = Modifier.fillMaxSize()) {
            CreamCard(modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    content()
                }
            }
        }
    }
}
