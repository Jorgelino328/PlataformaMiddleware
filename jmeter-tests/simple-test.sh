#!/bin/bash
echo "Testing middleware endpoints for JMeter compatibility..."
echo

# Test the basic endpoint
echo "1. Testing Calculator Add operation:"
curl -s -X POST -H "Content-Type: application/json" -d "[10, 20]" http://localhost:8082/invoke/CalculatorService/add
echo -e "\n"

# Test with different values (similar to JMeter random values)
echo "2. Testing with different values:"
curl -s -X POST -H "Content-Type: application/json" -d "[50, 75]" http://localhost:8082/invoke/CalculatorService/add
echo -e "\n"

echo "3. Testing multiply operation:"
curl -s -X POST -H "Content-Type: application/json" -d "[6, 7]" http://localhost:8082/invoke/CalculatorService/multiply
echo -e "\n"

echo "If all tests return numeric results, JMeter tests should work properly."
