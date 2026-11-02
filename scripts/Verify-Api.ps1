param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'

function Assert-Response {
    param([string]$Method, [string]$Path, [int]$ExpectedStatus, [object]$Body)
    $request = @{Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true}
    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = ConvertTo-Json -InputObject $Body
    }
    $response = $null
    try {
        $response = Invoke-WebRequest @request
        $actualStatus = [int]$response.StatusCode
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $actualStatus = [int]$_.Exception.Response.StatusCode
    }
    if ($actualStatus -ne $ExpectedStatus) {
        throw "$Method $Path expected $ExpectedStatus but returned $actualStatus."
    }
    Write-Host "PASS $Method $Path -> $actualStatus"
    return $response
}

$temporaryProductId = $null
try {
    $null = Assert-Response GET '/' 200
    $null = Assert-Response GET '/api/products' 200
    $response = Assert-Response POST '/api/products' 201 @{
        name = 'API Verification Product'
        description = 'Temporary product created by the verification script'
        price = 12.50
        stockQuantity = 3
    }
    $temporaryProductId = ($response.Content | ConvertFrom-Json).id
    $response = Assert-Response GET "/api/products/$temporaryProductId" 200
    $product = $response.Content | ConvertFrom-Json
    if ($product.name -ne 'API Verification Product' -or $product.stockQuantity -ne 3) {
        throw 'The saved product did not match the request.'
    }
    $response = Assert-Response PUT "/api/products/$temporaryProductId" 200 @{
        name = 'Updated Verification Product'; description = 'Updated'; price = 9.99; stockQuantity = 2
    }
    $updated = $response.Content | ConvertFrom-Json
    if ($updated.name -ne 'Updated Verification Product' -or $updated.price -ne 9.99) {
        throw 'The product update did not persist.'
    }
    $null = Assert-Response POST '/api/products' 400 @{
        name = ''; price = -1; stockQuantity = -1
    }
    $null = Assert-Response DELETE "/api/products/$temporaryProductId" 204
    $null = Assert-Response GET "/api/products/$temporaryProductId" 404
    $temporaryProductId = $null
    Write-Host 'All live API checks passed.'
} finally {
    if ($null -ne $temporaryProductId) {
        Invoke-WebRequest -Uri "$BaseUrl/api/products/$temporaryProductId" -Method DELETE -UseBasicParsing | Out-Null
    }
}
