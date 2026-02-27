Add-Type -AssemblyName System.Speech
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.SetOutputToWaveFile("src\main\resources\sounds\alert.wav")
$synth.Speak("beep beep beep")
$synth.Dispose()
Write-Host "alert.wav created successfully"
