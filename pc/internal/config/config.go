package config

import (
	"errors"
	"io"

	"gopkg.in/yaml.v3"
)

type Config struct {
	ServerIp string `yaml:"server_ip"`
	TcpPort  int    `yaml:"tcp_port"`
}

func LoadConfig(r io.Reader) (*Config, error) {
	var cfg Config
	decoder := yaml.NewDecoder(r)
	err := decoder.Decode(&cfg)
	if err != nil {
		return nil, err
	}
	err = validate(&cfg)
	if err != nil {
		return nil, errors.New("config validation failed: " + err.Error())
	}
	return &cfg, nil
}

func validate(cfg *Config) error {
	// cfg.ServerIp may be "", it's ok
	if cfg.TcpPort == 0 {
		return errors.New("tcp_port is required")
	}
	return nil
}

func (cfg *Config) SaveConfig(w io.Writer) error {
	encoder := yaml.NewEncoder(w)
	err := encoder.Encode(cfg)
	_ = encoder.Close()
	if err != nil {
		return err
	}
	return nil
}
