package br.com.anteros.persistence.session.configuration;

public class PackageScanEntity {

	protected String packageName;
	
	public PackageScanEntity(String packageName) {
		this.packageName = packageName;
	}

	public String getPackageName() {
		return packageName;
	}

	public PackageScanEntity setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	@Override
	public String toString() {
		return "PackageScanEntity [packageName=" + packageName + "]";
	}

}
