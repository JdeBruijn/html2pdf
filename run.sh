
rm -f *.class

jars="$(find -iname "*.jar" | tr '\n' ':' | sed 's/\.\///g')"

echo "jars=$jars"

javac -cp "$jars." *.java

if [ $? -gt 0 ]
then
	echo "Failed to compile"
	exit 1
fi

java -cp "$jars."  HtmlToPdfConverter $1 $2
