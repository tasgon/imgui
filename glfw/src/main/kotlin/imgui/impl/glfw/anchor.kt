package imgui.impl.glfw

import org.lwjgl.glfw.GLFW

val GLFW_HAS_GLFW_HOVERED = false

var GLFW_HAS_WINDOW_TOPMOST = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3200 // 3.2+ GLFW_FLOATING
var GLFW_HAS_WINDOW_HOVERED = false // GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3300 // 3.3+ GLFW_HOVERED
var GLFW_HAS_WINDOW_ALPHA = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3300 // 3.3+ glfwSetWindowOpacity
var GLFW_HAS_PER_MONITOR_DPI = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3300 // 3.3+ glfwGetMonitorContentScale
var GLFW_HAS_VULKAN = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3200 // 3.2+ glfwCreateWindowSurface
var GLFW_HAS_FOCUS_WINDOW = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3200 // 3.2+ glfwFocusWindow
var GLFW_HAS_FOCUS_ON_SHOW = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3300 // 3.3+ GLFW_FOCUS_ON_SHOW
var GLFW_HAS_MONITOR_WORK_AREA = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3300 // 3.3+ glfwGetMonitorWorkarea
var GLFW_HAS_OSX_WINDOW_POS_FIX = GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 + GLFW.GLFW_VERSION_REVISION * 10 >= 3310 // 3.3.1+ Fixed: Resizing window repositions it on MacOS #1553
var GLFW_HAS_NEW_CURSORS = false//GLFW.GLFW_VERSION_MAJOR * 1000 + GLFW.GLFW_VERSION_MINOR * 100 >= 3400 // 3.4+ GLFW_RESIZE_ALL_CURSOR, GLFW_RESIZE_NESW_CURSOR, GLFW_RESIZE_NWSE_CURSOR, GLFW_NOT_ALLOWED_CURSOR

val GLFW_MOUSE_PASSTHROUGH = false         // Let's be nice to people who pulled GLFW between 2019-04-16 (3.4 define) and 2020-07-17 (passthrough)
val GLFW_HAS_MOUSE_PASSTHROUGH = false //    (GLFW_VERSION_MAJOR * 1000 + GLFW_VERSION_MINOR * 100 >= 3400) // 3.4+ GLFW_MOUSE_PASSTHROUGH

var HAS_WIN32_IME = true