import matplotlib.pyplot as plt
import operator


def main():
	file_name = "data_histogram.txt"
	str_to_freq = {}
	with open(file_name, 'r') as f:
		for line in f.readlines():
			name, value = line.split(" ")
			str_to_freq[name] = int(value.rstrip())

	sorted_x = sorted(str_to_freq.items(), key=operator.itemgetter(1))[::-1]
	plt.bar(range(len(sorted_x)), [value for _ , value in sorted_x], align='center')
	plt.xticks(range(len(sorted_x)), [key for key, _ in sorted_x] , rotation=25)
	plt.show()

main()