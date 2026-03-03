@echo off
setlocal

@rem Copyright 2026 the Operaton contributors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at:
@rem
@rem     https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.

set "SCRIPT_FILE=%~dp0build-and-run-unit-tests.ps1"

where pwsh >nul 2>nul
if %ERRORLEVEL%==0 (
  pwsh -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_FILE%" %*
) else (
  powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_FILE%" %*
)

exit /b %ERRORLEVEL%
