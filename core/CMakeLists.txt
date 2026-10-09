cmake_minimum_required(VERSION 3.16)
project(pocket_sparrow_core VERSION 0.1.0 LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Optimization flags
if(NOT MSVC)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -Wall -Wextra -pthread")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /O2 /W4")
endif()

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/cpp
)

# Core sources
set(CORE_SOURCES
    cpp/detection_engine.cpp
)

# Shared and Static Library Targets
add_library(pocket_sparrow SHARED ${CORE_SOURCES})
add_library(pocket_sparrow_static STATIC ${CORE_SOURCES})

target_include_directories(pocket_sparrow PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>
)

target_include_directories(pocket_sparrow_static PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>
)

# Android NDK Integration
if(ANDROID)
    find_library(log-lib log)
    find_library(android-lib android)
    target_link_libraries(pocket_sparrow PUBLIC ${log-lib} ${android-lib})
    message(STATUS "Configured Pocket Sparrow for Android NDK target: ${ANDROID_ABI}")
endif()

# C++ Unit Test Runner
add_executable(test_sparrow_cpp
    cpp/tests/test_detection_engine.cpp
)
target_link_libraries(test_sparrow_cpp PRIVATE pocket_sparrow_static)

# C++ Benchmark Runner
add_executable(benchmark_sparrow_cpp
    cpp/tests/benchmark_detection_engine.cpp
)
target_link_libraries(benchmark_sparrow_cpp PRIVATE pocket_sparrow_static)

enable_testing()
add_test(NAME CoreCppTests COMMAND test_sparrow_cpp)
