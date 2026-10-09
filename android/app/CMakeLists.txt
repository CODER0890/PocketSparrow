cmake_minimum_required(VERSION 3.22.1)
project("pocketsparrow_native")

set(CMAKE_CXX_STANDARD 17)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../../core/include
)

add_library(
    pocket_sparrow
    SHARED
    src/main/cpp/native-lib.cpp
    ../../core/cpp/detection_engine.cpp
)

find_library(log-lib log)

target_link_libraries(
    pocket_sparrow
    ${log-lib}
)
