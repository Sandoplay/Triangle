[CmdletBinding()]
param(
    [ValidateSet('test', 'run')]
    [string]$Task = 'test',
    [string[]]$Sides = @()
)

$ErrorActionPreference = 'Stop'

foreach ($toolName in @('javac', 'java')) {
    if (-not (Get-Command $toolName -ErrorAction SilentlyContinue)) {
        throw "JDK 17 or newer is required. Add its bin directory to PATH."
    }
}

$outputDirectory = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$sourceFiles = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Filter '*.java' |
    Select-Object -ExpandProperty FullName)
if ($Task -eq 'test') {
    $sourceFiles += @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tests') -Filter '*.java' |
        Select-Object -ExpandProperty FullName)
}

& javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d $outputDirectory @sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed with exit code $LASTEXITCODE."
}

if ($Task -eq 'test') {
    & java -cp $outputDirectory TriangleTest
} else {
    & java -cp $outputDirectory Main @Sides
}
if ($LASTEXITCODE -ne 0) {
    throw "Execution failed with exit code $LASTEXITCODE."
}
