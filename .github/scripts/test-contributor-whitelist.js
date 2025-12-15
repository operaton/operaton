#!/usr/bin/env node
/**
 * Test script to validate the contributor whitelist workflow logic
 * This simulates the JavaScript logic used in the GitHub Actions workflow
 */

const fs = require('fs');
const path = require('path');

const whitelistPath = path.join(__dirname, '../contributor-whitelist.txt');

console.log('Testing contributor whitelist workflow logic...');
console.log('================================================\n');

// Test 1: Load whitelist
console.log('Test 1: Loading whitelist file...');
let whitelist = [];
try {
  const content = fs.readFileSync(whitelistPath, 'utf8');
  whitelist = content.split('\n')
    .map(line => line.trim())
    .filter(line => line && !line.startsWith('#'));
  console.log(`✓ Loaded ${whitelist.length} contributor(s) from whitelist\n`);
} catch (error) {
  console.error(`✗ Failed to load whitelist: ${error.message}`);
  process.exit(1);
}

// Test 2: Display loaded contributors
console.log('Test 2: Displaying loaded contributors...');
whitelist.forEach(username => {
  console.log(`  - ${username}`);
});
console.log('');

// Test 3: Simulate whitelist check function
console.log('Test 3: Testing whitelist check function...');
function isInWhitelist(username, whitelist) {
  return whitelist.includes(username);
}

const testCases = [
  { username: 'kthoms', expected: true, description: 'Known contributor' },
  { username: 'unknownuser', expected: false, description: 'Unknown contributor' },
  { username: '', expected: false, description: 'Empty username' },
  { username: 'KTHOMS', expected: false, description: 'Wrong case (case-sensitive check)' }
];

let passedTests = 0;
testCases.forEach(({ username, expected, description }) => {
  const result = isInWhitelist(username, whitelist);
  const status = result === expected ? '✓' : '✗';
  console.log(`  ${status} ${description}: isInWhitelist("${username}") = ${result}`);
  if (result === expected) passedTests++;
});
console.log('');

// Test 4: Simulate the workflow script logic
console.log('Test 4: Simulating workflow script logic...');
function simulateWorkflowCheck(author, whitelist) {
  console.log(`  Checking author: ${author}`);
  
  if (whitelist.includes(author)) {
    console.log(`  → ${author} is in the contributor whitelist. Skipping greeting.`);
    return false; // Skip greeting
  }
  
  console.log(`  → ${author} is not in whitelist. Would proceed with greeting check.`);
  return true; // Proceed with greeting
}

const workflowTests = [
  { author: 'kthoms', shouldGreet: false },
  { author: 'newcontributor', shouldGreet: true }
];

let workflowTestsPassed = 0;
workflowTests.forEach(({ author, shouldGreet }) => {
  const result = simulateWorkflowCheck(author, whitelist);
  const status = result === shouldGreet ? '✓' : '✗';
  console.log(`  ${status} Expected shouldGreet=${shouldGreet}, got=${result}\n`);
  if (result === shouldGreet) workflowTestsPassed++;
});

// Test 5: Validate Buffer.from decode (as used in workflow)
console.log('Test 5: Testing Base64 decode (simulating GitHub API response)...');
try {
  const originalContent = fs.readFileSync(whitelistPath, 'utf8');
  const base64Content = Buffer.from(originalContent).toString('base64');
  const decoded = Buffer.from(base64Content, 'base64').toString('utf8');
  
  const decodedWhitelist = decoded.split('\n')
    .map(line => line.trim())
    .filter(line => line && !line.startsWith('#'));
  
  if (JSON.stringify(decodedWhitelist) === JSON.stringify(whitelist)) {
    console.log('✓ Base64 encode/decode works correctly\n');
  } else {
    console.log('✗ Base64 encode/decode mismatch\n');
    process.exit(1);
  }
} catch (error) {
  console.error(`✗ Base64 test failed: ${error.message}`);
  process.exit(1);
}

// Summary
console.log('================================================');
const totalTests = testCases.length + workflowTests.length + 3; // +3 for load, display, base64
const totalPassed = passedTests + workflowTestsPassed + 3;

if (totalPassed === totalTests) {
  console.log('All tests passed! ✓\n');
  console.log('Summary:');
  console.log(`  - Whitelist file: ${whitelistPath}`);
  console.log(`  - Contributors: ${whitelist.length}`);
  console.log(`  - Tests passed: ${totalPassed}/${totalTests}`);
  process.exit(0);
} else {
  console.log(`Some tests failed: ${totalPassed}/${totalTests} passed\n`);
  process.exit(1);
}
