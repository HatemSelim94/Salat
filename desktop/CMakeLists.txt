cmake_minimum_required(VERSION 3.21)
project(salat VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

option(SALAT_BUILD_TESTS "Build the adhan port test" ON)

find_package(Qt6 6.4 REQUIRED COMPONENTS Core Gui Qml Quick QuickControls2 Widgets)
qt_standard_project_setup(REQUIRES 6.4)

# ---- algorithm (no Qt) ---------------------------------------------------------
add_library(adhan STATIC src/adhan/adhan.cpp src/adhan/adhan.h)
target_include_directories(adhan PUBLIC src)

# ---- app -----------------------------------------------------------------------
qt_add_executable(salat
    src/main.cpp
    src/Settings.cpp src/Settings.h
    src/Engine.cpp src/Engine.h
    src/Cities.cpp src/Cities.h
    src/Tray.cpp src/Tray.h
    src/Platform.cpp src/Platform.h
    src/Tr.cpp src/Tr.h
    src/About.h
)

# Theme.qml is a singleton (must be declared before qt_add_qml_module).
set_source_files_properties(qml/Theme.qml PROPERTIES QT_QML_SINGLETON_TYPE TRUE)

qt_add_qml_module(salat
    URI Salat
    VERSION 1.0
    QML_FILES
        qml/Main.qml
        qml/HomePage.qml
        qml/SettingsPage.qml
        qml/SettingsCard.qml
        qml/SettingRow.qml
        qml/LabeledField.qml
        qml/Theme.qml
    SOURCES
        src/Cities.h src/Cities.cpp
    RESOURCES
        resources/cities.json
        resources/translations.json
        resources/chime.wav
        resources/icons/icon16.png
        resources/icons/icon32.png
        resources/icons/icon48.png
        resources/icons/icon128.png
        resources/icons/icon256.png
    RESOURCE_PREFIX /
)

target_compile_definitions(salat PRIVATE SALAT_VERSION="${PROJECT_VERSION}")
target_link_libraries(salat PRIVATE adhan Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick Qt6::QuickControls2 Qt6::Widgets)
if(WIN32)
    target_link_libraries(salat PRIVATE winmm)
    set_target_properties(salat PROPERTIES WIN32_EXECUTABLE TRUE)
endif()
if(APPLE)
    set_target_properties(salat PROPERTIES
        MACOSX_BUNDLE TRUE
        MACOSX_BUNDLE_BUNDLE_NAME "Salat"
        MACOSX_BUNDLE_GUI_IDENTIFIER "io.github.hatemselim94.salat"
        MACOSX_BUNDLE_BUNDLE_VERSION "${PROJECT_VERSION}"
        MACOSX_BUNDLE_SHORT_VERSION_STRING "${PROJECT_VERSION}"
        MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/packaging/Info.plist.in"
    )
endif()

install(TARGETS salat BUNDLE DESTINATION . RUNTIME DESTINATION bin)
if(UNIX AND NOT APPLE)
    install(FILES packaging/salat.desktop DESTINATION share/applications)
    install(FILES resources/icons/icon256.png DESTINATION share/icons/hicolor/256x256/apps RENAME salat.png)
endif()

# ---- tests ---------------------------------------------------------------------
if(SALAT_BUILD_TESTS)
    enable_testing()
    add_executable(adhan_test tests/adhan_test.cpp)
    target_link_libraries(adhan_test PRIVATE adhan)
    add_test(NAME adhan_matches_core COMMAND adhan_test ${CMAKE_CURRENT_SOURCE_DIR}/tests/vectors.csv)
endif()
