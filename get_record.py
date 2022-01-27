import csv

file = open('results.txt', 'r')
lines = file.readlines()
total = 0
wins = 0
records = []
for line in lines:
    res = ""
    total += 1
    splitted = line.split()
    if splitted[2] != "MPTempName":
        res += "L "
    else:
        res += "W "
        wins += 1
    res += splitted[5]
    records.append(res)
csvFile = "records.csv"
with open(csvFile, 'w') as f:
    csvwriter = csv.writer(f) 
    csvwriter.writerow(records)
print(wins / total)