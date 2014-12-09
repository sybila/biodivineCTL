#include <vector>
#include <list>
#include <iostream>

class JVM {

	template<class T>
	class AnyInstance {
	protected:
		jobject _value;
		T * _type;
	public:
		AnyInstance(jobject value, T * classType) : _value(value), _type(classType) {}
		jobject object() { return _value; }
	};

	class AnyClass {
	protected:
		JNIEnv * _env;
		JVM * _jvm;
		jclass _class;
	public:
		AnyClass(JVM * jvm, const char * className) : 
			_jvm(jvm), 
			_env(jvm->_env),
			_class(_env->FindClass(className)) {};
	};

	JNIEnv * _env;

public:

	JNIEnv * getEnv() {
		return _env;
	}
	class DoubleClass : public AnyClass {
		jmethodID _valueOf;
		jmethodID _doubleValue;
	public:
		class Instance: public AnyInstance<DoubleClass> {
		public:
			Instance(jobject value, DoubleClass * type) : AnyInstance<DoubleClass>(value, type) {}
			double doubleValue() {
				return _type->_env->CallDoubleMethod(_value, _type->_doubleValue);
			}
		};
		DoubleClass::Instance valueOf(double d) {
			return DoubleClass::Instance(_env->CallStaticObjectMethod(_class, _valueOf, d), this);
		}
		DoubleClass( JVM * jvm ) : AnyClass(jvm, "java/lang/Double") {
			_valueOf = _env->GetStaticMethodID(_class, "valueOf", "(D)Ljava/lang/Double;");
			_doubleValue = _env->GetMethodID(_class, "doubleValue", "()D");
		}
	};

	class IntegerClass : public AnyClass {
		jmethodID _valueOf;
		jmethodID _intValue;
	public:
		class Instance: public AnyInstance<IntegerClass> {
		public:
			Instance(jobject value, IntegerClass * type) : AnyInstance(value, type) {}
			double intValue() {
				return _type->_env->CallDoubleMethod(_value, _type->_intValue);
			}
		};
		IntegerClass::Instance valueOf(int i) {
			return IntegerClass::Instance(_env->CallStaticObjectMethod(_class, _valueOf, i), this);
		}
		IntegerClass(JVM * jvm) : AnyClass(jvm, "java/lang/Integer") {
			_valueOf = _env->GetStaticMethodID(_class, "valueOf", "(I)Ljava/lang/Integer;");
			_intValue = _env->GetMethodID(_class, "intValue", "()I");
		}
	};

	class ListClass : public AnyClass {
		jmethodID _add;
		jmethodID _get;
		jmethodID _size;
	public:
		class Instance : public AnyInstance<ListClass> {
		public: 
			Instance(jobject value, ListClass * type) : AnyInstance(value, type) {}
			jboolean add(jobject item) {
				return _type->_env->CallBooleanMethod(_value, _type->_add, item);
			}
			jobject get(jint i) {
				return _type->_env->CallObjectMethod(_value, _type->_get, i);
			}
			jint size() {
				return _type->_env->CallIntMethod(_value, _type->_size);
			}
		};
		ListClass(JVM * jvm) : AnyClass(jvm, "java/util/List") {
			_get = _env->GetMethodID(_class, "get", "(I)Ljava/lang/Object;");
			_add = _env->GetMethodID(_class, "add", "(Ljava/lang/Object;)Z");               
			_size = _env->GetMethodID(_class, "size", "()I");
		}
	};

	class MapClass : public AnyClass {
		jmethodID _put;
		jmethodID _get;
		jmethodID _size;
		jmethodID _init;
	public:
		class Instance : public AnyInstance<MapClass> {
		public: 
			Instance(jobject value, MapClass * type) : AnyInstance(value, type) {}
			jobject put(jobject key, jobject item) {
				return _type->_env->CallObjectMethod(_value, _type->_put, key, item);
			}
			jobject get(jobject key) {
				return _type->_env->CallObjectMethod(_value, _type->_get, key);
			}
			jint size() {
				return _type->_env->CallIntMethod(_value, _type->_size);
			}
		};
		MapClass(JVM * jvm) : AnyClass(jvm, "java/util/Map") {
			_get = _env->GetMethodID(_class, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
			_put = _env->GetMethodID(_class, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
			_size = _env->GetMethodID(_class, "size", "()I");
		}
	};

	class NodeClass : public AnyClass {
		jfieldID _coordinates;
	public:
		class Instance : public AnyInstance<NodeClass> {
		public:
			Instance(jobject value, NodeClass * type) : 
				AnyInstance(value, type) {}
		};
		NodeClass(JVM * jvm) : AnyClass(jvm, "cz/muni/fi/distributed/graph/Node") {
		}
	};

	class NodeFactoryClass : public AnyClass {
		jmethodID _getNode;
	public:
		class Instance : public AnyInstance<NodeFactoryClass> {
		public:
			Instance(jobject value, NodeFactoryClass * type) : 
				AnyInstance(value, type) {}
			NodeClass::Instance getNode(jintArray coordinates) {
				jobject obj = _type->_env->CallObjectMethod(_value, _type->_getNode, coordinates);
				return NodeClass::Instance(obj, &(_type->_jvm->Node));
			}
		};
		NodeFactoryClass(JVM * jvm) : AnyClass(jvm, "cz/muni/fi/distributed/graph/NodeFactory") {
			_getNode = _env->GetMethodID(_class, "getNode", "([I)Lcz/muni/fi/distributed/graph/Node;");
		}
	};

	class ModelClass : public AnyClass {
		jfieldID _paramList;
		jfieldID _varList;
	public:
		class Instance : public AnyInstance<ModelClass> {
		public:
			ListClass::Instance paramList;
			ListClass::Instance varList;
			Instance(jobject value, ModelClass * type) : 
				AnyInstance(value, type), 
				paramList(_type->_env->GetObjectField(_value, _type->_paramList), &(_type->_jvm->List)),
				varList(_type->_env->GetObjectField(_value, _type->_varList), &(_type->_jvm->List)) {
			}
		};
		ModelClass(JVM * jvm) : AnyClass(jvm, "cz/muni/fi/model/Model") {
			_paramList = _env->GetFieldID(_class, "parameterRange", "Ljava/util/List;");        
			_varList = _env->GetFieldID(_class, "variableRange", "Ljava/util/List;");        
		}
	};

	class RangeClass : public AnyClass {		
		jmethodID _open;
		jmethodID _closed;
		jmethodID _upperEndPoint;
		jmethodID _lowerEndPoint;
	public:
		class Instance: public AnyInstance<RangeClass> {
		public:
			Instance(jobject value, RangeClass* type) : AnyInstance(value, type) {}
			DoubleClass::Instance upperEndPoint() {
				return DoubleClass::Instance(_type->_env->CallObjectMethod(_value, _type->_upperEndPoint), &(_type->_jvm->Double));
			}
			DoubleClass::Instance lowerEndPoint() {
				return DoubleClass::Instance(_type->_env->CallObjectMethod(_value, _type->_lowerEndPoint), &(_type->_jvm->Double));
			}
		};
		RangeClass(JVM * jvm) : AnyClass(jvm, "com/google/common/collect/Range") {
			_open = _env->GetStaticMethodID(_class, "open", "(Ljava/lang/Comparable;Ljava/lang/Comparable;)Lcom/google/common/collect/Range;");
			_closed = _env->GetStaticMethodID(_class, "closed", "(Ljava/lang/Comparable;Ljava/lang/Comparable;)Lcom/google/common/collect/Range;");
			_upperEndPoint = _env->GetMethodID(_class, "upperEndpoint", "()Ljava/lang/Comparable;");
			_lowerEndPoint = _env->GetMethodID(_class, "lowerEndpoint", "()Ljava/lang/Comparable;");
		}
		RangeClass::Instance open(double d1, double d2) {
			return open(_jvm->Double.valueOf(d1), _jvm->Double.valueOf(d2));
		}
		RangeClass::Instance open(DoubleClass::Instance d1, DoubleClass::Instance d2) {
			return RangeClass::Instance(_env->CallStaticObjectMethod(_class, _open, d1.object(), d2.object()), this);
		}
		RangeClass::Instance closed(double d1, double d2) {
			return closed(_jvm->Double.valueOf(d1), _jvm->Double.valueOf(d2));
		}
		RangeClass::Instance closed(DoubleClass::Instance d1, DoubleClass::Instance d2) {
			return RangeClass::Instance(_env->CallStaticObjectMethod(_class, _closed, d1.object(), d2.object()), this);
		}
	};

	class RangeSetClass : public AnyClass {
		jmethodID _asRanges;
		jmethodID _add;
	public:
		class Instance : public AnyInstance<RangeSetClass> {
		public:
			Instance(jobject value, RangeSetClass * type) : AnyInstance(value, type) {}
			jobject asRanges() {
				return _type->_env->CallObjectMethod(_value, _type->_asRanges);
			}
			void add(RangeClass::Instance range) {
				_type->_env->CallVoidMethod(_value, _type->_add, range.object());
			}
		};
		RangeSetClass(JVM * jvm) : AnyClass(jvm, "com/google/common/collect/RangeSet") {
			_asRanges = _env->GetMethodID(_class, "asRanges", "()Ljava/util/Set;");
			_add = _env->GetMethodID(_class, "add", "(Lcom/google/common/collect/Range;)V");
		}
	};

	class ColorSetClass : public AnyClass {
		jmethodID _size;
		jmethodID _get;
		jmethodID _createEmpty;
		jmethodID _asArrayForParam;
	public:
		class Instance : public AnyInstance<ColorSetClass> {
		public:
			Instance(jobject value, ColorSetClass* type) : AnyInstance(value, type) {}
			Instance(std::vector<std::list<std::pair<double, double>>> values, ColorSetClass * type) : 
				AnyInstance(type->createEmpty(values.size()), type) 
			{
				for ( int dim = 0; dim < values.size(); dim++ ) {
					auto rangeSet = get(dim);
					auto ranges = values[dim];
					auto  iter = ranges.begin();
					for( ; iter != ranges.end(); ++iter ) {						
						rangeSet.add(_type->_jvm->Range.closed(iter->first, iter->second));					
					}
				}
			}
			jint size() {
				return _type->_env->CallIntMethod(_value, _type->_size);
			}
			RangeSetClass::Instance get(jint i) {
				return RangeSetClass::Instance(_type->_env->CallObjectMethod(_value, _type->_get, i), &(_type->_jvm->RangeSet));
			}
			std::vector<std::list<std::pair<double,double> > > toParamSpace() {
				std::vector<std::list<std::pair<double, double> > > result;
				for ( int i=0; i < this->size(); i++ ) {
					std::list<std::pair<double, double> > list;
					jobjectArray rangeArray = (jobjectArray) _type->_env->CallObjectMethod(_value, _type->_asArrayForParam, i);
					jsize size = _type->_env->GetArrayLength(rangeArray);
					for (int i = 0; i < size; ++i)
					{
						jobject jRange = _type->_env->GetObjectArrayElement(rangeArray, i);
						RangeClass::Instance range = RangeClass::Instance(jRange, &(_type->_jvm->Range));
						list.push_back(std::pair<double, double>(range.lowerEndPoint().doubleValue(), range.upperEndPoint().doubleValue()));
					}
					result.push_back(list);
				}
				return result;
			}
		};
		ColorSetClass(JVM * jvm) : AnyClass(jvm, "cz/muni/fi/model/TreeColorSet") {
			_get = _env->GetMethodID(_class, "get", "(I)Ljava/lang/Object;");
			_size = _env->GetMethodID(_class, "size", "()I");
			_createEmpty = _env->GetStaticMethodID(_class, "createEmpty", "(I)Lcz/muni/fi/model/TreeColorSet;");
			_asArrayForParam = _env->GetMethodID(_class, "asArrayForParam", "(I)[Lcom/google/common/collect/Range;");
		}

		jobject createEmpty(jint size) {
			return _env->CallStaticObjectMethod(_class, _createEmpty, size);
		}
	};

	//numerals
	DoubleClass Double;
	IntegerClass Integer;
	//collections
	RangeClass Range;
	ListClass List;
	MapClass Map;
	NodeClass Node;
	NodeFactoryClass NodeFactory;
	ModelClass Model;
	RangeSetClass RangeSet;
	ColorSetClass ColorSet;

	JVM(JNIEnv * env) : _env(env),
		Double(this),
		Integer(this),
		Range(this),
		List(this),
		Map(this),
		Model(this),
		Node(this),
		NodeFactory(this),
		RangeSet(this),
		ColorSet(this)
	 { }

};