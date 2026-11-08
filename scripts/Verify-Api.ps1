param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'

function Assert-Response {
    param([string]$Method, [string]$Path, [int]$ExpectedStatus, [object]$Body)
    $request = @{Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true}
    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = ConvertTo-Json -InputObject $Body -Depth 4
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
$temporaryCategoryId = $null
try {
    $null = Assert-Response GET '/' 200
    $null = Assert-Response GET '/products-by-category.html' 200
    $null = Assert-Response GET '/manage-products.html' 200
    $null = Assert-Response GET '/api/categories' 200
    $null = Assert-Response GET '/api/products' 200

    $catResponse = Assert-Response POST '/api/categories' 201 @{
        name = 'Verification Plush Category'
        description = 'Temporary category created by the verification script'
    }
    $temporaryCategoryId = ($catResponse.Content | ConvertFrom-Json).id

    $catResponse = Assert-Response PUT "/api/categories/$temporaryCategoryId" 200 @{
        name = 'Updated Verification Plush Category'
        description = 'Updated temporary verification category'
    }
    $category = $catResponse.Content | ConvertFrom-Json
    if ($category.name -ne 'Updated Verification Plush Category') {
        throw 'The category update did not persist.'
    }

    $response = Assert-Response POST '/api/products' 201 @{
        name = 'API Verification Plushie'
        description = 'Temporary plushie created by the verification script'
        price = 12.50
        stockQuantity = 3
        category = @{ id = $temporaryCategoryId }
    }
    $temporaryProductId = ($response.Content | ConvertFrom-Json).id

    $response = Assert-Response GET "/api/products/$temporaryProductId" 200
    $product = $response.Content | ConvertFrom-Json
    if ($product.name -ne 'API Verification Plushie' -or $product.category.id -ne $temporaryCategoryId) {
        throw 'The saved product or category did not match the request.'
    }

    $filtered = (Assert-Response GET "/api/products?categoryId=$temporaryCategoryId" 200).Content | ConvertFrom-Json
    if ($filtered.Count -lt 1) {
        throw 'Category filter did not return the categorized product.'
    }

    $response = Assert-Response PUT "/api/products/$temporaryProductId" 200 @{
        name = 'Updated Verification Plushie'; description = 'Updated'; price = 9.99; stockQuantity = 2; category = $null
    }
    $updated = $response.Content | ConvertFrom-Json
    if ($updated.name -ne 'Updated Verification Plushie' -or $updated.price -ne 9.99 -or $null -ne $updated.category) {
        throw 'The product update did not persist.'
    }

    $null = Assert-Response GET '/api/products?uncategorized=true' 200
    $null = Assert-Response POST '/api/products' 400 @{
        name = ''; price = -1; stockQuantity = -1
    }
    $null = Assert-Response DELETE "/api/products/$temporaryProductId" 204
    $null = Assert-Response GET "/api/products/$temporaryProductId" 404
    $temporaryProductId = $null

    $null = Assert-Response DELETE "/api/categories/$temporaryCategoryId" 204
    $temporaryCategoryId = $null

    Write-Host 'All live API checks passed.'
} finally {
    if ($null -ne $temporaryProductId) {
        Invoke-WebRequest -Uri "$BaseUrl/api/products/$temporaryProductId" -Method DELETE -UseBasicParsing -ErrorAction SilentlyContinue | Out-Null
    }
    if ($null -ne $temporaryCategoryId) {
        Invoke-WebRequest -Uri "$BaseUrl/api/categories/$temporaryCategoryId" -Method DELETE -UseBasicParsing -ErrorAction SilentlyContinue | Out-Null
    }
}
