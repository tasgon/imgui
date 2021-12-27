package imgui.internal.api

import glm_.func.common.max
import glm_.parseInt
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.io
import imgui.api.g
import imgui.classes.DrawList
import imgui.font.Font
import imgui.internal.classes.ShrinkWidthItem
import imgui.internal.classes.Window
import imgui.internal.hash
import imgui.internal.sections.NavLayer
import imgui.statics.findWindowFocusIndex
import imgui.statics.navRestoreLastChildNavWindow
import uno.kotlin.NUL
import java.util.*
import java.util.regex.Pattern
import imgui.WindowFlag as Wf


@Suppress("UNCHECKED_CAST")

internal interface internal {

    // Windows

    /** We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
     *  If this ever crash because g.CurrentWindow is NULL it means that either
     *  - ImGui::NewFrame() has never been called, which is illegal.
     *  - You are calling ImGui functions after ImGui::EndFrame()/ImGui::Render() and before the next ImGui::NewFrame(), which is also illegal. */

    /** ~GetCurrentWindowRead */
    val currentWindowRead: Window?
        get() = g.currentWindow

    /** ~GetCurrentWindow */
    val currentWindow: Window
        get() = g.currentWindow?.apply { writeAccessed = true } ?: throw Error("""
                We should always have a CurrentWindow in the stack (there is an implicit \"Debug\" window)
                If this ever crash because ::currentWindow is NULL it means that either
                - ::newFrame() has never been called, which is illegal.
                - You are calling ImGui functions after ::render() and before the next ::newFrame(), which is also illegal.
                - You are calling ImGui functions after ::endFrame()/::render() and before the next ImGui::newFrame(), which is also illegal.
                """.trimIndent())

    fun findWindowByID(id: ID): Window? = g.windowsById[id]

    fun findWindowByName(name: String): Window? = g.windowsById[hash(name)]


    // Windows: Display Order and Focus Order

    /** Moving window to front of display (which happens to be back of our sorted list)  ~ FocusWindow  */
    fun focusWindow(window: Window? = null) {

        if (g.navWindow !== window) {
            g.navWindow = window
            if (window != null && g.navDisableMouseHover)
                g.navMousePosDirty = true
            g.navInitRequest = false
            g.navId = window?.navLastIds?.get(0) ?: 0 // Restore NavId
            g.navFocusScopeId = 0
            g.navIdIsAlive = false
            g.navLayer = NavLayer.Main
            //IMGUI_DEBUG_LOG("FocusWindow(\"%s\")\n", window ? window->Name : NULL);
        }

        // Close popups if any
        closePopupsOverWindow(window, false)

        // Move the root window to the top of the pile
        assert(window == null || window.rootWindow != null)
        val focusFrontWindow = window?.rootWindow // NB: In docking branch this is window->RootWindowDockStop
        val displayFrontWindow = window?.rootWindow

        // Steal active widgets. Some of the cases it triggers includes:
        // - Focus a window while an InputText in another window is active, if focus happens before the old InputText can run.
        // - When using Nav to activate menu items (due to timing of activating on press->new window appears->losing ActiveId)
        if (g.activeId != 0 && g.activeIdWindow?.rootWindow !== focusFrontWindow)
            if (!g.activeIdNoClearOnFocusLoss)
                clearActiveID()

        // Passing NULL allow to disable keyboard focus
        if (window == null)
            return

        // Bring to front
        focusFrontWindow!!.bringToFocusFront()
        if ((window.flags or displayFrontWindow!!.flags) hasnt Wf.NoBringToFrontOnFocus)
            displayFrontWindow.bringToDisplayFront()
    }

    fun focusTopMostWindowUnderOne(underThisWindow: Window? = null, ignoreWindow: Window? = null) {
        var startIdx = g.windowsFocusOrder.lastIndex
        underThisWindow?.let {
            val underThisWindowIdx = findWindowFocusIndex(it)
            if (underThisWindowIdx != -1)
                startIdx = underThisWindowIdx - 1
        }
        for (i in startIdx downTo 0) {
            // We may later decide to test for different NoXXXInputs based on the active navigation input (mouse vs nav) but that may feel more confusing to the user.
            val window = g.windowsFocusOrder[i]
            if (window !== ignoreWindow && window.wasActive && window.flags hasnt Wf._ChildWindow)
                if ((window.flags and (Wf.NoMouseInputs or Wf.NoNavInputs)) != (Wf.NoMouseInputs or Wf.NoNavInputs)) {
                    focusWindow(navRestoreLastChildNavWindow(window))
                    return
                }
        }
        focusWindow()
    }

    // the rest of the window related functions is inside the corresponding class


    // Fonts, drawing

    fun setCurrentFont(font: Font) {
        assert(font.isLoaded) { "Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?" }
        assert(font.scale > 0f)
        g.font = font
        g.fontBaseSize = 1f max (io.fontGlobalScale * g.font.fontSize * g.font.scale)
        g.fontSize = g.currentWindow?.calcFontSize() ?: 0f

        val atlas = g.font.containerAtlas
        g.drawListSharedData.also {
            it.texUvWhitePixel = atlas.texUvWhitePixel
            it.texUvLines = atlas.texUvLines
            it.font = g.font
            it.fontSize = g.fontSize
        }
    }

    /** ~GetDefaultFont */
    val defaultFont: Font
        get() = io.fontDefault ?: io.fonts.fonts[0]

    fun getForegroundDrawList(window: Window?): DrawList {
        return g.foregroundDrawList // This seemingly unnecessary wrapper simplifies compatibility between the 'master' and 'docking' branches.
    }


    val formatArgPattern: Pattern
        get() = Pattern.compile("%(\\d+\\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])")

    fun parseFormatFindStart(fmt: String): Int {
        val matcher = formatArgPattern.matcher(fmt)
        var i = 0
        while (matcher.find(i)) {
            if (fmt[matcher.end() - 1] != '%')
                return matcher.start()
            i = matcher.end()
        }
        return 0
    }

    /** We don't use strchr() because our strings are usually very short and often start with '%' */
    fun parseFormatFindStart2(fmt_: String): Int {
        var fmt = StringPointer(fmt_)
        var c = fmt[0]
        while (c != NUL) {
            if (c == '%' && fmt[1] != '%')
                return fmt()
            else if (c == '%')
                fmt++
            fmt++
            c = fmt[0]
        }
        return fmt()
    }

    fun parseFormatFindEnd(fmt: String, i_: Int = 0): Int {
        val matcher = formatArgPattern.matcher(fmt)
        var i = 0
        while (matcher.find(i)) {
            if (fmt[matcher.end() - 1] != '%')
                return matcher.end()
            i = matcher.end()
        }
        return 0
    }

    /** Extract the format out of a format string with leading or trailing decorations
     *  fmt = "blah blah"  -> return fmt
     *  fmt = "%.3f"       -> return fmt
     *  fmt = "hello %.3f" -> return fmt + 6
     *  fmt = "%.3f hello" -> return buf written with "%.3f" */
    fun parseFormatTrimDecorations(fmt: String): String {
        val fmtStart = parseFormatFindStart(fmt)
        if (fmt[fmtStart] != '%')
            return fmt
        val fmtEnd = fmtStart + parseFormatFindEnd(fmt.substring(fmtStart))
        return fmt.substring(fmtStart, fmtEnd)
    }

    /** Parse display precision back from the display format string
     *  FIXME: This is still used by some navigation code path to infer a minimum tweak step, but we should aim to rework widgets so it isn't needed. */
    fun parseFormatPrecision(fmt: String, defaultPrecision: Int): Int {
        var i = parseFormatFindStart(fmt)
        if (fmt[i] != '%')
            return defaultPrecision
        i++
        while (fmt[i] in '0'..'9')
            i++
        var precision = Int.MAX_VALUE
        if (fmt[i] == '.') {
            val s = fmt.substring(i).filter { it.isDigit() }
            if (s.isNotEmpty()) {
                precision = s.parseInt()
                if (precision < 0 || precision > 99)
                    precision = defaultPrecision
            }
        }
        if (fmt[i].toLowerCase() == 'e')    // Maximum precision with scientific notation
            precision = -1
        if (fmt[i].toLowerCase() == 'g' && precision == Int.MAX_VALUE)
            precision = -1
        return when (precision) {
            Int.MAX_VALUE -> defaultPrecision
            else -> precision
        }
    }

    companion object {

        fun alphaBlendColor(colA: Int, colB: Int): Int {
            val t = ((colB ushr COL32_A_SHIFT) and 0xFF) / 255f
            val r = imgui.internal.lerp((colA ushr COL32_R_SHIFT) and 0xFF, (colB ushr COL32_R_SHIFT) and 0xFF, t)
            val g = imgui.internal.lerp((colA ushr COL32_G_SHIFT) and 0xFF, (colB ushr COL32_G_SHIFT) and 0xFF, t)
            val b = imgui.internal.lerp((colA ushr COL32_B_SHIFT) and 0xFF, (colB ushr COL32_B_SHIFT) and 0xFF, t)
            return COL32(r, g, b, 0xFF)
        }

        val shrinkWidthItemComparer: Comparator<ShrinkWidthItem> = compareBy(ShrinkWidthItem::width, ShrinkWidthItem::index)
    }
}